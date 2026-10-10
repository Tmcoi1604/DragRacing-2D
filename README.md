# DragRacing-2D

Game đua xe 2D trên Android mang phong cách arcade, tập trung vào cảm giác phóng nhanh, chuyển số, nitro và chế độ Career. Người chơi có thể chọn xe, nâng cấp xe, mua xe mới, cá nhân hóa màu sơn và tham gia nhiều dạng đua với AI đối thủ.

## Tổng quan

DragRacing-2D là một tựa game đua xe 2D chạy trên Android, sử dụng Java + Android SDK. Game tích hợp các tính năng chính như:

- Đua nhanh theo nhiều khoảng cách và độ khó khác nhau.
- Chế độ Career với stage và boss race.
- Gara xe với hệ thống sở hữu, chọn xe và nâng cấp.
- Nhiều cảnh đường đua khác nhau và background phong phú.
- AI đối thủ có phản ứng, nitro và chuyển số tự động.
- Lưu tiến độ người chơi và kỷ lục thời gian chạy tốt nhất.

## Tính năng chính

### 1. Chế độ đua

- Quick Race: đua nhanh với tùy chọn khoảng cách, độ khó và môi trường.
- Career Mode: giải đấu theo từng stage, đối thủ và boss race.
- Test Run: chạy thử xe mà không cần đối thủ, phục vụ kiểm tra hiệu suất.

### 2. Hệ thống đua và điều khiển

- Khoảng cách đua: 1/4 dặm, 1/2 dặm, 1 dặm, 2 dặm.
- Hệ thống start tree (Christmas Tree) với đèn đếm ngược.
- Phát hiện false start khi nhả ga quá sớm.
- Đạp ga, phanh, chuyển số và nhấn nitro theo nhịp hợp lý.
- AI đối thủ di chuyển và lên số theo RPM đồng thời có thể sử dụng nitro.
- Kết quả race hiển thị thời gian chạy, tốc độ tối đa và trạng thái thắng/thua.

### 3. Xe và dữ liệu kỹ thuật

Game có nhiều mẫu xe thuộc các hạng xe khác nhau:

- Tier 1: xe thể thao nhập môn
- Tier 2: xe thể thao / GT
- Tier 3: xe hiệu suất cao
- Tier 4: xe siêu xe và xe track-focused
- Tier 5: siêu xe flagship

Mỗi xe có các thông số như:

- Horsepower (mã lực)
- Weight (trọng lượng)
- Grip (độ bám đường)
- Shift time (thời gian chuyển số)
- RPM tối đa và vùng RPM tối ưu
- Gear ratios và final drive
- Body type

### 4. Hệ thống nâng cấp xe

Người chơi có thể nâng cấp mỗi xe theo 6 hạng mục:

- Engine
- Turbo
- Nitro
- Tires
- Gearbox
- Weight

Mỗi loại nâng cấp có tối đa 5 cấp độ; giá nâng cấp được tính dựa trên giá xe và cấp độ hiện tại.

### 5. Gara và cá nhân hóa xe

- Chọn màn hình Garage để xem danh sách xe.
- Mua xe mới bằng tiền trong game.
- Chọn xe đang dùng làm xe chính.
- Thay đổi màu sơn xe từ bảng màu có sẵn.
- Xem các chỉ số thực tế của xe sau khi nâng cấp và sơn lại.
- Duy trì tiến độ sở hữu xe qua nhiều lần chơi.

### 6. Chế độ Career

Career là hệ thống tiến bộ dài hạn:

- Mỗi stage có đối thủ và màn hình preview tương ứng.
- Có stage thường và boss race ở cuối giai đoạn.
- Tiền thưởng và tiến độ Career được lưu lại trong SharedPreferences.
- Người chơi tiến triển qua các stage với cấp độ khó tăng dần.
- Đối thủ trong Career có thể dựa trên các xe trong danh sách và được điều chỉnh theo mức độ khó.

### 7. Môi trường đường đua

Game có 10 bối cảnh đường đua khác nhau, bao gồm:

1. Thành phố đêm
2. Khu công nghiệp
3. Sa mạc
4. Đại lộ cao tốc
5. Đường đua ven biển
6. Núi tuyết và cực quang
7. Hầm tàu điện graffiti
8. Khu rừng bình minh
9. Đường đua lễ hội
10. Đường đua chuyên dụng

Mỗi môi trường có background riêng, phông cảnh và cảm giác không khí rất khác nhau.

### 8. AI và độ khó

- Độ khó hỗ trợ: Dễ, Thường, Khó, Rất Khó.
- AI có phản ứng khởi động, tốc độ lên số và sử dụng nitro theo mức khó.
- Boss race mạnh hơn đáng kể so với đối thủ bình thường.
- Độ khó ảnh hưởng trực tiếp đến sức mạnh AI và thời gian phản ứng.

### 9. Tiền và lưu tiến độ

- Người chơi có tiền khởi đầu và có thể kiếm thêm qua các cuộc đua.
- Hệ thống `PlayerData` lưu:
  - Số tiền hiện có
  - Xe đang chọn
  - Xe đã sở hữu
  - Tiến độ Career
  - Kỷ lục thời gian theo khoảng cách
- Dữ liệu được lưu trên thiết bị thông qua SharedPreferences.

### 10. Giao diện và âm thanh

- Giao diện game có phong cách retro / pixel UI.
- Màn hình chính có preview xe và tùy chọn đua nhanh.
- Có nút bật/tắt âm thanh.
- SoundManager quản lý tiếng động cơ, hiss nitro và hiệu ứng âm thanh khi tăng tốc.

## Luồng chơi cơ bản

1. Mở game và chọn chế độ đua.
2. Chọn xe và kiểm tra thông số trong Garage.
3. Nâng cấp và sơn lại nếu cần thiết.
4. Lựa chọn độ khó, khoảng cách và môi trường.
5. Bắt đầu cuộc đua, chờ đèn xanh và phóng đi.
6. Quản lý RPM, bấm số hợp lý và kích hoạt nitro đúng thời điểm.
7. Hoàn thành race để kiếm tiền, nâng cấp và tiến thêm trong Career.

## Cấu trúc dự án

```text
DragRacing-2D/
├── app/
│   ├── build.gradle.kts             # Cấu hình module Android và dependency
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  # Khai báo application, activity và permission
│           ├── java/com/dragracing/game/
│           │   ├── MainActivity.java
│           │   ├── audio/
│           │   │   └── SoundManager.java
│           │   ├── data/
│           │   │   ├── Car.java
│           │   │   ├── CarDatabase.java
│           │   │   └── PlayerData.java
│           │   ├── engine/
│           │   │   ├── CarPhysics.java
│           │   │   └── RaceEngine.java
│           │   ├── render/
│           │   │   ├── CarRenderer.java
│           │   │   └── TrackRenderer.java
│           │   └── ui/
│           │       ├── CarPreviewView.java
│           │       ├── CareerActivity.java
│           │       ├── GameActivity.java
│           │       ├── GameView.java
│           │       ├── GarageActivity.java
│           │       ├── PauseDialog.java
│           │       └── ResultDialog.java
│           └── res/
│               ├── drawable/        # Hình ảnh xe, background và drawable XML
│               ├── font/            # Font pixel của game
│               ├── layout/          # Layout XML cho Activity, item và dialog
│               └── values/          # String, color, ID và theme
├── gradle/
│   ├── libs.versions.toml           # Quản lý version dependency/plugin
│   └── wrapper/                     # Gradle Wrapper
├── build.gradle.kts                 # Cấu hình plugin cấp project
├── settings.gradle.kts              # Tên project và module được include
├── gradle.properties
├── gradlew / gradlew.bat            # Chạy Gradle không cần cài Gradle riêng
├── map tilesets/                    # Tài nguyên tileset của bản đồ
├── cars_ascii.txt
├── wheels_fit.txt
├── wheel_specs_generated.java.txt
└── README.md
```

### Các thành phần chính

- `data`: mô hình xe, danh sách xe và dữ liệu tiến trình người chơi.
- `engine`: xử lý vật lý xe, logic đua và hệ thống timing / nitro / AI.
- `render`: vẽ xe, đường đua, background và cảnh quan từng môi trường.
- `ui`: màn hình chính, gara, Career, game scene và dialog kết quả/tạm dừng.
- `audio`: quản lý âm thanh động cơ và hiệu ứng nitro.
- `res`: tài nguyên, hình ảnh, font và layout Android.

## Yêu cầu môi trường

- Android Studio hỗ trợ Android Gradle Plugin `9.2.1`.
- JDK 11.
- Android SDK Platform 36.1 (compile SDK).
- Thiết bị thật hoặc Android Emulator có Android API 24 trở lên.

## Mở và chạy ứng dụng

1. Mở Android Studio và chọn **Open**.
2. Chọn thư mục gốc `DragRacing-2D` (thư mục chứa `settings.gradle.kts`).
3. Chờ Android Studio hoàn tất **Gradle Sync**. Nếu được hỏi, chọn JDK 11 cho Gradle.
4. Chọn cấu hình chạy `app`, chọn một Android Emulator hoặc thiết bị thật.
5. Nhấn **Run** để build và cài ứng dụng.

Build từ terminal:

```powershell
.\gradlew.bat assembleDebug
```

APK debug sẽ được tạo trong `app/build/outputs/apk/debug/`.

## Chạy test trên Android Studio

### 1. Chuẩn bị

- Mở project và chờ Gradle Sync hoàn tất.
- Kết nối thiết bị Android đã bật USB debugging hoặc khởi động Emulator.
- Nếu tạo test mới, đặt file Java trong:
  - `app/src/test/java/` cho unit test JVM.
  - `app/src/androidTest/java/` cho instrumented test.

### 2. Chạy local unit test

```powershell
.\gradlew.bat testDebugUnitTest
```

### 3. Chạy instrumented test

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### 4. Kiểm tra thiết bị

```powershell
adb devices
```

## Kết luận

DragRacing-2D là một game đua xe 2D arcade hoàn chỉnh với gameplay đa dạng, hệ thống gara, Career, nâng cấp, nhiều xe, nhiều môi trường và AI đối thủ. Đây là dự án phù hợp cho việc học Android Game Development, mô hình dữ liệu game, UI, vật lý đơn giản và quản lý tiến độ người chơi.
