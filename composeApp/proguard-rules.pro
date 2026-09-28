# Compose와 Koin은 각자 consumer 규칙을 들고 오므로 여기에 따로 적지 않는다.
# 직렬화 모델이 붙으면 kotlinx.serialization 규칙을 여기에 추가한다.

# WorkManager 2.10이 끌어오는 Room 2.6은 DB 클래스만 남기고 기본 생성자는 남기지 않는다. R8 full mode는 그 생성자를
# 지워서 첫 수집을 맡기는 WorkManager가 앱을 켜자마자 죽는다(NoSuchMethodException: WorkDatabase_Impl.<init>).
# Room 2.7부터는 라이브러리 규칙에 생성자가 들어 있어서, WorkManager가 Room 2.7 이상을 끌어오면 이 줄을 지운다.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
