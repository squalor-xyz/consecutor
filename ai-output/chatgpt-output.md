Below is an example **Android** project structure and sample implementation for **Consecutor**. The sample code uses **Kotlin**, **Android Jetpack (ViewModel, LiveData, Room)**, and some common best practices to ensure a robust, maintainable, and easily portable codebase. It also includes encryption stubs, unit tests, build/deployment instructions, documentation files, and an MIT license.

---

## 1. Tagline and Short Description

**Tagline**: _Track your streaks, stay consistent. Consecutor helps you keep going, one day at a time._

**Short Description**:  
Consecutor is a simple, intuitive app that helps you track events or habits on a daily basis. With a single tap, you can increment a streak counter to see how many consecutive days you’ve maintained your activity. Miss a day? Your streak resets — but Consecutor still keeps historical data so you can see your week, month, and year stats.

---

## 2. Project Structure Overview

A typical Android Studio project structure might look like this:

```
consecutor/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── androidTest/
│       │   └── ...
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/
│       │   │   └── com/squalor/consecutor/
│       │   │       ├── ConsecutorApp.kt
│       │   │       ├── data/
│       │   │       │   ├── db/
│       │   │       │   │   ├── EventDao.kt
│       │   │       │   │   ├── EventDatabase.kt
│       │   │       │   │   └── Converters.kt
│       │   │       │   ├── model/
│       │   │       │   │   └── Event.kt
│       │   │       │   └── repository/
│       │   │       │       └── EventRepository.kt
│       │   │       ├── ui/
│       │   │       │   ├── add/
│       │   │       │   │   └── AddEventActivity.kt
│       │   │       │   ├── main/
│       │   │       │   │   ├── MainActivity.kt
│       │   │       │   │   ├── EventListAdapter.kt
│       │   │       │   │   └── EventViewModel.kt
│       │   │       │   └── details/
│       │   │       │       └── EventDetailsActivity.kt
│       │   │       └── util/
│       │   │           └── EncryptionUtil.kt
│       │   ├── res/
│       │   │   ├── layout/
│       │   │   │   ├── activity_main.xml
│       │   │   │   ├── item_event.xml
│       │   │   │   └── activity_add_event.xml
│       │   │   ├── values/
│       │   │   │   ├── colors.xml
│       │   │   │   ├── dimens.xml
│       │   │   │   ├── strings.xml
│       │   │   │   └── themes.xml
│       │   └── ...
│       └── test/
│           └── ...
├── docs/
│   ├── REQUIREMENTS.md
│   ├── PRIVACY.md
│   ├── HELP.md
│   └── ...
├── LICENSE
├── README.md
└── build.gradle
```

Below, you’ll find sample code and files that demonstrate how the various pieces can fit together. This is a minimal but illustrative example. In a real production project, you may need to add more classes, error handling, analytics, etc.

---

## 3. Build Files

### 3.1. **Root** `build.gradle`

```groovy
// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

### 3.2. **Module** `app/build.gradle`

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
}

android {
    namespace 'com.squalor.consecutor'
    compileSdk 34

    defaultConfig {
        applicationId "com.squalor.consecutor"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            // Debug config
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.10"

    // AndroidX Core
    implementation "androidx.core:core-ktx:1.9.0"
    implementation "androidx.appcompat:appcompat:1.6.1"

    // Material Design
    implementation "com.google.android.material:material:1.9.0"

    // Lifecycle & LiveData & ViewModel
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.5.1"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1"

    // Room
    implementation "androidx.room:room-runtime:2.5.1"
    kapt "androidx.room:room-compiler:2.5.1"
    implementation "androidx.room:room-ktx:2.5.1"

    // Encryption
    implementation "androidx.security:security-crypto:1.1.0-alpha04"

    // Testing
    testImplementation "junit:junit:4.13.2"
    androidTestImplementation "androidx.test.ext:junit:1.1.5"
    androidTestImplementation "androidx.test.espresso:espresso-core:3.5.1"
}
```

---

## 4. AndroidManifest

```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.squalor.consecutor">

    <application
        android:name=".ConsecutorApp"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.Consecutor">
        
        <!-- Main Activity -->
        <activity android:name=".ui.main.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Add Event Activity -->
        <activity android:name=".ui.add.AddEventActivity" />
        
        <!-- Event Details Activity -->
        <activity android:name=".ui.details.EventDetailsActivity" />

    </application>

</manifest>
```

Notable points:
- `android:allowBackup="false"` is set to `false` for stricter local-only data usage (until you enable an official backup or cloud sync feature).
- A reference to `ConsecutorApp` (application class).

---

## 5. Application Class

```kotlin
// app/src/main/java/com/squalor/consecutor/ConsecutorApp.kt
package com.squalor.consecutor

import android.app.Application
import androidx.room.Room
import com.squalor.consecutor.data.db.EventDatabase

class ConsecutorApp : Application() {

    companion object {
        lateinit var database: EventDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Room. For encryption, you can integrate net.sqlcipher or a similar encrypted DB library.
        database = Room.databaseBuilder(
            applicationContext,
            EventDatabase::class.java,
            "consecutor_db"
        )
            .fallbackToDestructiveMigration() // handle DB migrations carefully in a real app
            .build()
    }
}
```

Here you can see a typical pattern of building and holding a reference to `EventDatabase`. For more secure storage, you could incorporate **SQLCipher for Android** or other solutions.

---

## 6. Data Layer

### 6.1. `Event.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/data/model/Event.kt
package com.squalor.consecutor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String? = null,
    val label: String? = null,
    val folder: String? = null,
    val lastIncrementDate: Date? = null,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalCount: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
```

- `lastIncrementDate` is used to check if a new day has passed (for consecutive day checks).
- `currentStreak`, `maxStreak`, and `totalCount` track usage statistics.
- `createdAt` and `updatedAt` can be used for housekeeping or future analytics.

### 6.2. `EventDao.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/data/db/EventDao.kt
package com.squalor.consecutor.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.squalor.consecutor.data.model.Event

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM events ORDER BY updatedAt DESC")
    fun getAllEvents(): LiveData<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): Event?
}
```

### 6.3. `Converters.kt` (for Date type)

```kotlin
// app/src/main/java/com/squalor/consecutor/data/db/Converters.kt
package com.squalor.consecutor.data.db

import androidx.room.TypeConverter
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
```

### 6.4. `EventDatabase.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/data/db/EventDatabase.kt
package com.squalor.consecutor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.squalor.consecutor.data.model.Event

@Database(
    entities = [Event::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
```

---

## 7. Repository

```kotlin
// app/src/main/java/com/squalor/consecutor/data/repository/EventRepository.kt
package com.squalor.consecutor.data.repository

import androidx.lifecycle.LiveData
import com.squalor.consecutor.ConsecutorApp
import com.squalor.consecutor.data.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EventRepository {

    private val eventDao = ConsecutorApp.database.eventDao()

    fun getAllEvents(): LiveData<List<Event>> {
        return eventDao.getAllEvents()
    }

    suspend fun getEventById(id: Long): Event? {
        return withContext(Dispatchers.IO) {
            eventDao.getEventById(id)
        }
    }

    suspend fun insertEvent(event: Event) {
        withContext(Dispatchers.IO) {
            eventDao.insertEvent(event)
        }
    }

    suspend fun updateEvent(event: Event) {
        withContext(Dispatchers.IO) {
            eventDao.updateEvent(event)
        }
    }

    suspend fun deleteEvent(event: Event) {
        withContext(Dispatchers.IO) {
            eventDao.deleteEvent(event)
        }
    }
}
```

---

## 8. Utility Class for Encryption

Below is a stub for local encryption. In a real-world scenario, you might use **EncryptedSharedPreferences** or a more robust solution.  
```kotlin
// app/src/main/java/com/squalor/consecutor/util/EncryptionUtil.kt
package com.squalor.consecutor.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

object EncryptionUtil {

    fun encryptFile(context: Context, sourceBytes: ByteArray, fileName: String) {
        val mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val file = File(context.filesDir, fileName)
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { output ->
            output.write(sourceBytes)
        }
    }

    fun decryptFile(context: Context, fileName: String): ByteArray? {
        val mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null

        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileInput().use { input ->
            return input.readBytes()
        }
    }
}
```

If you wish to store the entire DB encrypted, you’d use **SQLCipher** or a similar library for Room. 

---

## 9. UI Layer

### 9.1. `MainActivity.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/ui/main/MainActivity.kt
package com.squalor.consecutor.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squalor.consecutor.R
import com.squalor.consecutor.databinding.ActivityMainBinding
import com.squalor.consecutor.ui.add.AddEventActivity
import com.squalor.consecutor.ui.details.EventDetailsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EventViewModel by viewModels()
    private lateinit var adapter: EventListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = EventListAdapter { event ->
            // When an event item is tapped -> increment the streak
            viewModel.incrementStreak(event)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Floating button to add a new event
        findViewById<FloatingActionButton>(R.id.fabAddEvent).setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        // Observe changes
        viewModel.allEvents.observe(this) { events ->
            adapter.submitList(events)
        }
    }
}
```

**`ActivityMainBinding`** is generated from `activity_main.xml`. For illustration:

```xml
<!-- app/src/main/res/layout/activity_main.xml -->
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_alignParentTop="true"/>

        <com.google.android.material.floatingactionbutton.FloatingActionButton
            android:id="@+id/fabAddEvent"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_alignParentEnd="true"
            android:layout_alignParentBottom="true"
            android:layout_margin="16dp"
            android:contentDescription="@string/add_event"
            android:src="@drawable/ic_baseline_add_24" />
    </RelativeLayout>
</layout>
```

### 9.2. `EventListAdapter.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/ui/main/EventListAdapter.kt
package com.squalor.consecutor.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squalor.consecutor.R
import com.squalor.consecutor.data.model.Event
import com.squalor.consecutor.databinding.ItemEventBinding

class EventListAdapter(
    private val onEventClicked: (Event) -> Unit
) : ListAdapter<Event, EventListAdapter.EventViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, onEventClicked)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onEventClicked: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvEventName.text = event.name
            binding.tvStreak.text = itemView.context.getString(
                R.string.current_streak_format, 
                event.currentStreak
            )
            binding.tvEmoji.text = event.emoji ?: ""

            itemView.setOnClickListener {
                onEventClicked(event)
            }
        }
    }
}
```

**`ItemEventBinding`** is generated from `item_event.xml`. For illustration:

```xml
<!-- app/src/main/res/layout/item_event.xml -->
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp">

        <TextView
            android:id="@+id/tvEmoji"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="24sp"
            android:layout_marginEnd="16dp"/>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvEventName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceBody1"/>

            <TextView
                android:id="@+id/tvStreak"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceCaption" />
        </LinearLayout>

    </LinearLayout>
</layout>
```

### 9.3. `EventViewModel.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/ui/main/EventViewModel.kt
package com.squalor.consecutor.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squalor.consecutor.data.model.Event
import com.squalor.consecutor.data.repository.EventRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class EventViewModel : ViewModel() {

    val allEvents: LiveData<List<Event>> = EventRepository.getAllEvents()

    fun incrementStreak(event: Event) {
        viewModelScope.launch {
            val today = resetToDayStart(Calendar.getInstance()).time
            val lastIncrement = event.lastIncrementDate?.let {
                resetToDayStart(Calendar.getInstance().apply { time = it }).time
            }

            val newEvent = if (lastIncrement == null) {
                // first increment
                event.copy(
                    lastIncrementDate = today,
                    currentStreak = 1,
                    maxStreak = 1,
                    totalCount = event.totalCount + 1,
                    updatedAt = Date()
                )
            } else {
                val diff = daysBetween(lastIncrement, today)
                if (diff == 0) {
                    // same day - do not increment consecutive day (or do you want to prevent multiple increments per day?)
                    // For this example, let's allow multiple increments in a single day if user desires
                    event.copy(
                        currentStreak = event.currentStreak + 1,
                        maxStreak = maxOf(event.maxStreak, event.currentStreak + 1),
                        totalCount = event.totalCount + 1,
                        updatedAt = Date()
                    )
                } else if (diff == 1) {
                    // consecutive day
                    val newStreak = event.currentStreak + 1
                    event.copy(
                        lastIncrementDate = today,
                        currentStreak = newStreak,
                        maxStreak = maxOf(event.maxStreak, newStreak),
                        totalCount = event.totalCount + 1,
                        updatedAt = Date()
                    )
                } else {
                    // streak broken
                    event.copy(
                        lastIncrementDate = today,
                        currentStreak = 1,
                        maxStreak = maxOf(event.maxStreak, event.currentStreak),
                        totalCount = event.totalCount + 1,
                        updatedAt = Date()
                    )
                }
            }

            EventRepository.updateEvent(newEvent)
        }
    }

    private fun resetToDayStart(calendar: Calendar): Calendar {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    private fun daysBetween(start: Date, end: Date): Int {
        val diff = end.time - start.time
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}
```

### 9.4. `AddEventActivity.kt`

```kotlin
// app/src/main/java/com/squalor/consecutor/ui/add/AddEventActivity.kt
package com.squalor.consecutor.ui.add

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.squalor.consecutor.R
import com.squalor.consecutor.data.model.Event
import com.squalor.consecutor.ui.main.EventViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddEventActivity : AppCompatActivity() {

    private val viewModel: EventViewModel by viewModels()
    private lateinit var etEventName: EditText
    private lateinit var etEmoji: EditText
    private lateinit var etLabel: EditText
    private lateinit var btnAddEvent: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        etEventName = findViewById(R.id.etEventName)
        etEmoji = findViewById(R.id.etEmoji)
        etLabel = findViewById(R.id.etLabel)
        btnAddEvent = findViewById(R.id.btnAddEvent)

        btnAddEvent.setOnClickListener {
            val name = etEventName.text.toString().trim()
            val emoji = etEmoji.text.toString().trim()
            val label = etLabel.text.toString().trim()

            if (name.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val newEvent = Event(name = name, emoji = emoji, label = label)
                    viewModel.insertStreakEvent(newEvent)
                }
                finish()
            } else {
                // Show a validation error
            }
        }
    }
}

// We can augment the existing EventViewModel with this helper:
fun EventViewModel.insertStreakEvent(event: Event) {
    viewModelScope.launch {
        com.squalor.consecutor.data.repository.EventRepository.insertEvent(event)
    }
}
```

**`activity_add_event.xml`** might be:
```xml
<!-- app/src/main/res/layout/activity_add_event.xml -->
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical" 
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <EditText
        android:id="@+id/etEventName"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/event_name_hint"/>

    <EditText
        android:id="@+id/etEmoji"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/emoji_hint"/>

    <EditText
        android:id="@+id/etLabel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/label_hint"/>

    <Button
        android:id="@+id/btnAddEvent"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/add_event"/>
</LinearLayout>
```

### 9.5. `EventDetailsActivity.kt` (Optional)

You might have an Activity to show details, stats over time, etc. In practice, you’d read an `eventId` from an Intent, fetch from the DB, display, and allow the user to archive/delete.

---

## 10. CSV Export

You could add a simple function in your `EventRepository` or any specialized utility that fetches all events and writes them into a CSV file, or directly returns the string for the user to share.

Example snippet:

```kotlin
// CSVExportUtil.kt
package com.squalor.consecutor.util

import com.squalor.consecutor.data.model.Event

object CSVExportUtil {

    fun exportEventsToCSV(events: List<Event>): String {
        // Construct CSV
        val sb = StringBuilder()
        sb.append("id,name,emoji,label,folder,currentStreak,maxStreak,totalCount,createdAt,updatedAt\n")
        for (e in events) {
            sb.append("${e.id},")
            sb.append("\"${e.name}\",")
            sb.append("\"${e.emoji ?: ""}\",")
            sb.append("\"${e.label ?: ""}\",")
            sb.append("\"${e.folder ?: ""}\",")
            sb.append("${e.currentStreak},")
            sb.append("${e.maxStreak},")
            sb.append("${e.totalCount},")
            sb.append("${e.createdAt.time},")
            sb.append("${e.updatedAt.time}\n")
        }
        return sb.toString()
    }
}
```

---

## 11. Unit Tests

Example unit test for `EventViewModel`:

```kotlin
// app/src/test/java/com/squalor/consecutor/ui/main/EventViewModelTest.kt
package com.squalor.consecutor.ui.main

import com.squalor.consecutor.data.model.Event
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class EventViewModelTest {

    @Test
    fun testDaysBetween() = runTest {
        val vm = EventViewModel()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.time
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.time

        val diff = vmTestDaysBetween(vm, start, end)
        assertEquals(1, diff)
    }

    @Test
    fun testIncrementStreak_noPrevious() = runTest {
        val vm = EventViewModel()
        val event = Event(name = "Test Event")
        vm.incrementStreak(event)
        // This is a simplified test; in real usage, you'd mock the repo or check updated event from DB
        // ... Insert logic or mocking ...
    }

    // Helper function to access private method for testing
    private fun vmTestDaysBetween(vm: EventViewModel, start: java.util.Date, end: java.util.Date): Int {
        val method = EventViewModel::class.java.getDeclaredMethod("daysBetween", java.util.Date::class.java, java.util.Date::class.java)
        method.isAccessible = true
        return method.invoke(vm, start, end) as Int
    }
}
```

In a real project, you’d typically mock the repository and verify results.

---

## 12. Requirements Specification Document (REQUIREMENTS.md)

Create a file `docs/REQUIREMENTS.md`:

```markdown
# Consecutor Requirements

## Overview
Consecutor is an Android application that lets users track events or habits by incrementing a daily counter. If a day is missed, the consecutive counter resets.

## Functional Requirements
1. **Create Events**: The user must be able to create new events with:
   - A title (name)
   - An emoji (optional)
   - A label (optional)
   - A folder/category (optional)
   - A daily reminder notification (optional, future expansion)

2. **Increment Event**: Tapping an event in the list increments its counter. If the user:
   - Increments on the same day, the consecutive counter increases.  
   - Increments on the next day, the streak continues.
   - Skips a day, the consecutive counter resets.

3. **Track Statistics**: For each event, keep:
   - Current streak
   - Max streak
   - Total count (over all time)
   - Historical logs (optional expansions)

4. **Local Data**: All data is stored locally on the device in an encrypted database. No external data sync is performed unless user explicitly enables a future “cloud backup”.

5. **Export Data**: The user can export a CSV file with all event data.

6. **Delete/Archive Events**: The user can swipe to archive or delete an event. 

7. **Folders/Labels**: The user can organize events in folders/labels for better grouping.

8. **UI**: The interface is a modern mobile layout with a list of events on the main screen.

9. **Notifications**: The user can enable or disable daily reminders (Placeholder for future version).

10. **Security**: 
    - Local data is encrypted.
    - The user must be presented with a privacy notice stating that no data is collected externally.

## Non-Functional Requirements
1. **Performance**: Incrementing an event or loading the list should be near-instant.
2. **Portability**: The app is written in Kotlin with minimal Android-specific code so it can be ported to iOS (using a shared code approach if needed).
3. **Maintainability**: The codebase follows MVVM architecture, with a repository pattern to separate concerns.
4. **Reliability**: Edge cases around missing consecutive days, same-day increments, and database migrations must be handled gracefully.
5. **Open Source**: Licensed under MIT. Code repository on GitHub: [https://github.com/squalor-xyz/consecutor](https://github.com/squalor-xyz/consecutor)

```

---

## 13. Privacy Notice (PRIVACY.md)

```markdown
# Consecutor Privacy Notice

**Effective Date**: 2025-01-01

Consecutor does not collect, store, or transmit any user data off the device by default. All information is stored locally on your device in an encrypted database. We do not track usage analytics, nor do we share any personal information with third parties.

If the user opts to enable an external backup or sync feature (planned future feature), user consent and a separate permission step will be required.

For questions, contact: privacy@squalor-llc.com
```

---

## 14. Help Documentation (HELP.md)

```markdown
# Consecutor Help Guide

1. **Creating an Event**:
   - Tap the **+** button on the main screen.
   - Enter a name, optional emoji, and label.
   - Tap **Add**.

2. **Incrementing a Streak**:
   - On the main list, tap any event. Your streak count for that day will increment.
   - If you skip a day, your streak will reset to 1 next time you tap.

3. **Deleting / Archiving**:
   - Swipe left or right on an event to delete or archive. (If you archive, it’s hidden from the main list.)

4. **Export CSV**:
   - In the future, a button or menu option will allow you to export a CSV file of events.

For additional questions, please see our repository or contact support@squalor-llc.com
```

---

## 15. MIT License (LICENSE)

```text
MIT License

Copyright (c) 2025 Squalor, LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

[Standard MIT License text continues...]
```

*(Keep the full standard MIT text in your repo.)*

---

## 16. README.md

```markdown
# Consecutor

**Tagline**: _Track your streaks, stay consistent._

Consecutor is an Android application that allows you to track daily events or habits with ease. Every time you increment an event, Consecutor updates your streak. If you skip a day, your streak resets automatically. The app stores all data locally on the device and never transmits it without your permission.

![Consecutor Demo Screenshot](docs/images/demo.png) _(example screenshot)_

## Features
- Create/label events with optional emoji/folder.
- Tap to increment streak counters.
- Automatic streak resets after missed days.
- View total count, current streak, and max streak.
- Export data as CSV.
- Organized by folders and labels.
- **No** external data collection.

## Getting Started
1. Clone the repository:  
   ```bash
   git clone https://github.com/squalor-xyz/consecutor.git
   ```
2. Open in **Android Studio**.
3. Build and run on an emulator or physical device (minSdk 21).

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss potential modifications.

## License
[MIT License](LICENSE)
```

---

## 17. Example HTML Landing Page

You might host a simple landing page at `consecutor/index.html` or on a website:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <title>Consecutor - The Streak Tracking App</title>
  <meta name="description" content="Consecutor helps you track daily events with a consecutive counter.">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
  <header>
    <h1>Consecutor</h1>
    <p><em>Track your streaks, stay consistent.</em></p>
  </header>

  <section>
    <p>Consecutor is an open-source Android application that tracks your daily events or habits.</p>
    <p><a href="https://github.com/squalor-xyz/consecutor">View on GitHub</a></p>
  </section>

  <footer>
    <p>&copy; 2025 Squalor, LLC</p>
  </footer>
</body>
</html>
```

---

## 18. Build and Deploy Instructions

### 18.1. Building Locally (Android Studio / Gradle)

1. **Clone the Repo**: `git clone https://github.com/squalor-xyz/consecutor.git`
2. **Open in Android Studio**:
   - Use File > Open, select the `consecutor` folder.
   - Let Gradle synchronize.
3. **Run/Debug**:
   - Select an emulator or connected device and press “Run”.

### 18.2. Using Jenkins (or Other CI)

A simple Jenkins pipeline could be:

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/squalor-xyz/consecutor.git'
            }
        }
        stage('Build') {
            steps {
                sh './gradlew assembleDebug'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Assemble Release') {
            steps {
                sh './gradlew assembleRelease'
            }
        }
    }
}
```

- Make sure your Jenkins environment has the correct **JDK** and **Android SDK** installed. 
- The pipeline clones the repository, builds the debug version, runs unit tests, and assembles a release APK.

### 18.3. Free/Low-Cost Alternatives
- **GitHub Actions**: You can create a GitHub Actions workflow for free (private repos have usage limits, but public repos are generally free for many open-source projects).
- **Bitrise**: Offers a free tier for small teams and open-source projects.

---

# Final Thoughts

This sample shows an illustrative approach to building **Consecutor** in Kotlin/Android:
- **MVVM architecture** with a `ViewModel`, `Repository`, and `Room` for local database.
- **Encryption** placeholders to protect local data.
- Basic **unit tests** and references to a CI pipeline for deployment.

From here, you can extend the application with additional features like push notifications, an iOS port (using technologies like Kotlin Multiplatform or a separate Swift codebase), and cloud backups.
