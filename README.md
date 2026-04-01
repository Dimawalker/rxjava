# Custom RxJava Implementation

##  Содержание
1. Архитектура системы
2. Принципы работы Scheduler
3. Процесс тестирования
4. Примеры использования

---

##  Архитектура системы

### Общая структура

Система построена на основе паттерна "Наблюдатель" (Observer) и состоит из следующих ключевых компонентов:


│ Observable<T> │

│ (Издатель событий) │

│ 
│  • Хранит функцию подписки OnSubscribe<T> 

│  • Предоставляет операторы для трансформации потока 

│  • Управляет потоками выполнения через Schedulers 

│ 

│
│ subscribe()

↓

│ Observer<T> │

│ (Подписчик) │

│ 
│ │ • onNext(T) - получает элементы 

│ │ • onError(Throwable) - обрабатывает ошибки 

│ │ • onComplete() - завершение потока

│ 

│
│ использует

↓

│ Scheduler │

│ (Планировщик потоков) │
│ 

│ │ • IOScheduler - для IO операций

│ │ • ComputationScheduler - для CPU вычислений 

│ │ • SingleScheduler - для последовательных операций 

 Принципы работы Schedulers
Общая концепция
Schedulers в реактивном программировании управляют потоками выполнения. Они определяют, в каком потоке будут выполняться различные части реактивной цепочки.

Типы Schedulers
1. IOScheduler (IO планировщик)
   Характеристики:

Использует CachedThreadPool

Создает новые потоки по мере необходимости

Переиспользует idle потоки (60 секунд)

Неограниченное количество потоков (ограничено системными ресурсами)

Области применения:

Сетевые запросы (HTTP, WebSocket)

Чтение/запись файлов

Операции с базой данных

Любые блокирующие IO операции

2. ComputationScheduler (Вычислительный планировщик)
   Характеристики:

Использует FixedThreadPool

Количество потоков = количеству ядер CPU

Оптимизирован для CPU-интенсивных операций

Не подходит для блокирующих операций

Области применения:

Математические вычисления

Обработка данных (парсинг, валидация)

Шифрование/дешифрование

Алгоритмы машинного обучения

3. SingleScheduler (Однопоточный планировщик)

   Характеристики:

Использует один поток

Гарантирует последовательное выполнение

Поддерживает очередь задач

Сохраняет порядок обработки

Области применения:

 Обновление UI (аналог Android Main Thread)

Операции, требующие последовательности

Работа с общим состоянием

Логирование

Процесс тестирования
Стратегия тестирования
Тестирование построено на JUnit 5 и охватывает все ключевые компоненты системы.

Основные сценарии тестирования
1. Тестирование базовых компонентов
   Что проверяется:

Корректная передача всех элементов

Вызов onComplete после последнего элемента

Отсутствие ошибок

2. Тестирование операторов

   Что проверяется:

map: преобразование значений

filter: корректная фильтрация

Цепочка операторов: последовательное применение

3. Тестирование Schedulers

   Что проверяется:

subscribeOn: правильный поток подписки

observeOn: правильный поток обработки

Комбинация планировщиков

4. Тестирование обработки ошибок

   Что проверяется:

Перехват ошибок

Восстановление потока после ошибки

Альтернативные значения

5. Тестирование многопоточности

   Что проверяется:

Безопасность потоков

Отсутствие race conditions

Правильная синхронизация

6. Тестирование Disposable

   Что проверяется:

Отмена подписки

Прекращение получения элементов

Проверка состояния

Примеры использования
Пример 1: Загрузка пользователей с API
java
public class UserService {
    
    public Observable<User> fetchUsers(List<Integer> userIds) {
        return Observable.fromIterable(userIds)
            .subscribeOn(Scheduler.io())           // IO для сетевых запросов
            .flatMap(id -> fetchUserFromApi(id))   // Асинхронные запросы
            .observeOn(Scheduler.single())         // UI поток для обновления
            .onErrorResumeNext(error -> {          // Обработка ошибок
                System.err.println("Error: " + error);
                return Observable.just(User.empty());
            });
    }
    
    private Observable<User> fetchUserFromApi(int id) {
        return Observable.create(observer -> {
            // Симуляция API запроса
            Thread.sleep(1000);
            observer.onNext(new User(id, "User" + id));
            observer.onComplete();
        });
    }
}

// Использование
userService.fetchUsers(Arrays.asList(1, 2, 3, 4, 5))
    .subscribe(
        user -> System.out.println("Loaded: " + user),
        error -> System.err.println("Failed: " + error),
        () -> System.out.println("All users loaded")
    );
Пример 2: Обработка больших данных
java
public class DataProcessor {
    
    public Observable<Result> processLargeDataset(List<Data> dataset) {
        return Observable.fromIterable(dataset)
            .subscribeOn(Scheduler.computation())    // CPU-интенсивная обработка
            .map(this::transformData)                // Трансформация
            .filter(this::validateData)              // Валидация
            .buffer(1000)                            // Группировка по 1000
            .map(this::aggregateData)                // Агрегация
            .observeOn(Scheduler.io())               // Сохранение в БД
            .flatMap(this::saveToDatabase);          // IO операция
    }
    
    private Data transformData(Data data) {
        // Сложные вычисления
        return data.transform();
    }
    
    private Observable<Result> saveToDatabase(List<Data> batch) {
        return Observable.create(observer -> {
            // Сохранение в БД
            Result result = database.save(batch);
            observer.onNext(result);
            observer.onComplete();
        });
    }
}
Пример 3: Real-time обработка событий
java
public class EventProcessor {
    
    private final PublishSubject<Event> eventStream = PublishSubject.create();
    
    public Observable<Alert> processEvents() {
        return eventStream
            .subscribeOn(Scheduler.io())
            .filter(event -> event.getSeverity() > 5)     // Только важные
            .window(5, TimeUnit.SECONDS)                  // Окна по 5 секунд
            .flatMap(window -> 
                window.reduce((e1, e2) -> e1.merge(e2))   // Объединение
            )
            .map(this::analyzeWindow)                      // Анализ
            .filter(alert -> alert.isCritical())           // Только критические
            .observeOn(Scheduler.single())                 // UI уведомления
            .doOnNext(alert -> sendNotification(alert));   // Отправка
    }
    
    public void onEvent(Event event) {
        eventStream.onNext(event);
    }
}
Пример 4: Параллельная обработка файлов
java
public class FileProcessor {
    
    public Observable<ProcessingResult> processFiles(List<String> filePaths) {
        return Observable.fromIterable(filePaths)
            .flatMap(filePath -> 
                Observable.just(filePath)
                    .subscribeOn(Scheduler.computation())
                    .map(this::readFile)           // Чтение
                    .map(this::parseFile)           // Парсинг
                    .map(this::processContent)      // Обработка
                    .onErrorResumeNext(error -> {
                        System.err.println("Error processing " + filePath);
                        return Observable.just(ProcessingResult.failed(filePath));
                    })
            )
            .observeOn(Scheduler.single())
            .doOnComplete(() -> System.out.println("All files processed"));
    }
    
    private FileContent readFile(String path) {
        // Чтение файла
        return new FileContent(path);
    }
}
Пример 5: Комплексная обработка ошибок
java
public class RobustService {
    
    public Observable<Response> callWithRetry(Request request) {
        return Observable.just(request)
            .retry(3)                                    // Повтор 3 раза
            .flatMap(this::callExternalService)          // Вызов сервиса
            .timeout(5, TimeUnit.SECONDS)                // Таймаут 5 сек
            .onErrorResumeNext(error -> {
                if (error instanceof TimeoutException) {
                    return Observable.just(Response.timeout());
                } else if (error instanceof IOException) {
                    return Observable.just(Response.retryLater());
                } else {
                    return Observable.error(error);
                }
            })
            .doOnNext(response -> log.info("Response: {}", response))
            .doOnError(error -> log.error("Error: {}", error));
    }
}

Разработанная библиотека RxJava предоставляет:

Преимущества
Реактивность: Асинхронная обработка потоков данных

Многопоточность: Гибкое управление потоками через Schedulers

Композиционность: Цепочки операторов для сложных преобразований

Отказоустойчивость: Механизмы обработки ошибок

Управляемость: Отмена подписок через Disposable
