# NotificationHub - Technical Documentation

## 📖 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Database Schema](#database-schema)
4. [Permission System](#permission-system)
5. [Background Services](#background-services)
6. [Security Implementation](#security-implementation)
7. [Performance Optimization](#performance-optimization)
8. [Testing Strategy](#testing-strategy)
9. [Build & Deployment](#build--deployment)

## 🏗️ Architecture Overview

### MVVM Architecture Pattern
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   View Layer    │────│  ViewModel      │────│  Repository     │
│   (Fragments)   │    │   Layer         │    │    Layer        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                               ┌─────────────────┐
                                               │  Data Sources   │
                                               │ (Room Database) │
                                               └─────────────────┘
```

### Package Structure
```
com.notificationhub/
├── data/
│   ├── database/           # Room database components
│   ├── entity/             # Data entities
│   ├── repository/         # Repository implementations
│   └── converter/          # Type converters
├── service/                # Background services
├── ui/
│   ├── fragment/          # UI fragments
│   ├── adapter/           # RecyclerView adapters
│   ├── viewmodel/         # ViewModels
│   └── dialog/            # Custom dialogs
├── utils/                 # Utility classes
├── receiver/              # Broadcast receivers
└── SmartNotifyApplication.kt  # Application class
```

## 🔧 Core Components

### 1. SmartNotificationListenerService
**Purpose**: Main notification interception and processing service

```kotlin
class SmartNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Filter system notifications
        // 2. Extract notification data
        // 3. Check app importance
        // 4. Store important notifications
        // 5. Trigger re-alert if needed
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handle notification dismissal
    }
}
```

**Key Features**:
- Real-time notification interception
- Intelligent filtering based on app importance
- Notification deduplication
- Integration with Room database
- Battery-optimized processing

### 2. ReAlertService
**Purpose**: Manages background re-alert scheduling

```kotlin
class ReAlertService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleNextReAlert()
        return START_STICKY
    }
    
    private fun scheduleNextReAlert() {
        // 1. Check for unread important notifications
        // 2. Respect DND and silent mode settings
        // 3. Schedule next alert using AlarmManager
        // 4. Show summary notification if needed
    }
}
```

**Features**:
- Configurable re-alert intervals (1-60 minutes)
- DND awareness
- Silent mode support
- Battery-optimized scheduling

### 3. Repository Layer

#### NotificationRepository
```kotlin
class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val encryptionHelper: EncryptionHelper
) {
    suspend fun insertNotification(notification: NotificationEntity) {
        notificationDao.insert(encryptionHelper.encrypt(notification))
    }
    
    fun getAllNotifications(): LiveData<List<NotificationEntity>> {
        return notificationDao.getAllNotifications().map { list ->
            list.map { encryptionHelper.decrypt(it) }
        }
    }
}
```

#### AppPreferenceRepository
```kotlin
class AppPreferenceRepository(
    private val appPreferenceDao: AppPreferenceDao
) {
    suspend fun updateAppImportance(packageName: String, isImportant: Boolean) {
        appPreferenceDao.updateImportance(packageName, isImportant)
    }
}
```

## 🗄️ Database Schema

### Room Database Configuration
```kotlin
@Database(
    entities = [
        NotificationEntity::class,
        AppPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appPreferenceDao(): AppPreferenceDao
}
```

### Entity Definitions

#### NotificationEntity
```kotlin
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isImportant: Boolean = false,
    val largeIcon: ByteArray? = null,
    val category: String? = null
)
```

#### AppPreferenceEntity
```kotlin
@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isImportant: Boolean = false,
    val icon: ByteArray? = null,
    val lastNotificationTime: Long = 0,
    val notificationCount: Int = 0
)
```

### DAOs (Data Access Objects)

#### NotificationDao
```kotlin
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): LiveData<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isImportant = 1 AND isRead = 0")
    fun getUnreadImportantNotifications(): LiveData<List<NotificationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long)
}
```

## 🔐 Permission System

### Required Permissions
```xml
<!-- Core notification access -->
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />

<!-- Post notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Background services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Device wake for re-alerts -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Battery optimization exemption -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Alarm scheduling -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<!-- Boot receiver -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Package discovery -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### Permission Helper Implementation
```kotlin
object PermissionHelper {
    fun checkAllPermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            canPostNotifications = canPostNotifications(context),
            hasNotificationListenerAccess = isNotificationListenerEnabled(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        )
    }
    
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, SmartNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver, 
            "enabled_notification_listeners"
        )
        return flat?.contains(cn.flattenToString()) == true
    }
}
```

## ⚙️ Background Services

### Service Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    System Notifications                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│         SmartNotificationListenerService                   │
│  • Intercepts notifications                                │
│  • Filters based on app importance                         │
│  • Stores in Room database                                 │
│  • Triggers re-alert scheduling                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 ReAlertService                              │
│  • Runs as foreground service                              │
│  • Schedules re-alerts using AlarmManager                  │
│  • Respects DND and user preferences                       │
│  • Shows summary notifications                             │
└─────────────────────────────────────────────────────────────┘
```

### Background Processing Strategy
1. **NotificationListenerService**: Always running when permission granted
2. **Foreground Service**: Only when re-alerts are active
3. **AlarmManager**: For precise re-alert timing
4. **WorkManager**: For maintenance tasks (cleanup, etc.)

## 🔒 Security Implementation

### Data Encryption
```kotlin
class EncryptionHelper {
    private val keyAlias = "NotificationHubKey"
    
    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_GCM + "/"
                + KeyProperties.ENCRYPTION_PADDING_NONE)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return Base64.encodeToString(cipher.doFinal(data.toByteArray()), Base64.DEFAULT)
    }
    
    private fun getSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}
```

### Privacy Protection
- **No Network Access**: Zero network permissions
- **Local Storage Only**: All data in encrypted Room database
- **No Analytics**: No tracking or data collection
- **Minimal Permissions**: Only essential permissions requested

## ⚡ Performance Optimization

### Memory Management
```kotlin
class NotificationAdapter : RecyclerView.Adapter<NotificationViewHolder>() {
    private var notifications = emptyList<NotificationEntity>()
    
    fun updateNotifications(newNotifications: List<NotificationEntity>) {
        val diffCallback = NotificationDiffCallback(notifications, newNotifications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notifications = newNotifications
        diffResult.dispatchUpdatesTo(this)
    }
}
```

### Database Optimization
- **Indexing**: Proper indexes on frequently queried columns
- **Pagination**: Load notifications in chunks
- **Background Cleanup**: Automatic old data deletion
- **Efficient Queries**: Optimized SQL queries with Room

### Battery Optimization
```kotlin
class ReAlertService : Service() {
    private val wakeLock by lazy {
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NotificationHub::ReAlertWakeLock"
        )
    }
    
    private fun showReAlert() {
        wakeLock.acquire(30_000) // 30 seconds max
        try {
            // Show notification
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
```

## 🧪 Testing Strategy

### Unit Tests
```kotlin
@Test
fun `test notification filtering logic`() {
    val notificationProcessor = NotificationProcessor()
    val testNotification = createTestNotification("com.whatsapp")
    
    val result = notificationProcessor.shouldShowNotification(testNotification)
    
    assertTrue(result)
}
```

### Integration Tests
```kotlin
@Test
fun `test database operations`() = runTest {
    val notification = NotificationEntity(
        id = "test_id",
        packageName = "com.test",
        appName = "Test App",
        title = "Test Title",
        text = "Test Content",
        timestamp = System.currentTimeMillis()
    )
    
    notificationDao.insert(notification)
    val retrieved = notificationDao.getNotificationById("test_id")
    
    assertEquals(notification, retrieved)
}
```

### Manual Testing Checklist
- [ ] Notification reception and filtering
- [ ] Re-alert functionality
- [ ] Permission flows
- [ ] Settings persistence
- [ ] Battery optimization
- [ ] Dark theme support
- [ ] Multi-language support

## 🚀 Build & Deployment

### Build Configuration
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.notificationhub"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
    }
}
```

### ProGuard Configuration
```proguard
# Keep Room entities
-keep class com.notificationhub.data.entity.** { *; }

# Keep NotificationListenerService
-keep class com.notificationhub.service.SmartNotificationListenerService { *; }

# Keep essential methods
-keepclassmembers class ** {
    @androidx.room.* <methods>;
}
```

### Release Process
1. **Version Bump**: Update versionCode and versionName
2. **Testing**: Run full test suite
3. **Build**: Generate signed release APK
4. **Upload**: Upload to Play Console
5. **Staged Rollout**: Start with 20% rollout
6. **Monitor**: Watch crash reports and user feedback
7. **Full Rollout**: Complete rollout after validation

### Signing Configuration
```gradle
signingConfigs {
    release {
        storeFile file('keystore/release.keystore')
        storePassword System.getenv("KEYSTORE_PASSWORD")
        keyAlias System.getenv("KEY_ALIAS")
        keyPassword System.getenv("KEY_PASSWORD")
    }
}
```

## 📊 Monitoring & Analytics

### Crash Reporting
- Use Play Console's built-in crash reporting
- No third-party analytics for privacy reasons
- Local error logging for debugging

### Performance Monitoring
- Battery usage monitoring
- Memory leak detection
- Database performance tracking

## 🔄 Maintenance

### Regular Tasks
- **Database Cleanup**: Remove old notifications based on retention settings
- **Permission Validation**: Ensure permissions are still granted
- **Service Health**: Monitor background service status
- **Performance Review**: Regular performance audits

### Update Strategy
- **Semantic Versioning**: MAJOR.MINOR.PATCH
- **Backward Compatibility**: Maintain database migration paths
- **Gradual Rollout**: Staged releases for major updates
- **User Communication**: Clear release notes for significant changes

---

**This technical documentation provides a comprehensive overview of NotificationHub's architecture, implementation details, and maintenance procedures for developers and system administrators.**