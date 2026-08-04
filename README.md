# TestPilot Studio

TestPilot Studio la ung dung desktop JavaFX de chay UI/E2E test tren website theo du lieu Input/Expected Output trong Excel. Ung dung mo Chromium bang Playwright, thao tac nhu nguoi dung, cap nhat tien do, chup anh, quay video, luu trace va xuat bao cao Excel.

## Cong nghe

- Java 11 va JavaFX 17
- Playwright Java 1.61
- Apache POI cho Excel
- SQLite luu project, chuc nang va lich su tien trinh
- Maven, phu hop mo truc tiep bang IntelliJ IDEA

## Chuc nang da co trong MVP

- Tao project, moi project co nhieu chuc nang can test.
- Import va kiem tra sheet `Automation Steps`.
- Ho tro 15 action: `goto`, `click`, `fill`, `press`, `select`, `check`, `uncheck`, `upload`, `wait`, `expectText`, `expectVisible`, `expectHidden`, `expectUrl`, `expectRowsContain`, `screenshot`.
- Chay Chromium co giao dien hoac headless.
- Thay bien `${BASE_URL}`, `${USERNAME}`, `${PASSWORD}` ma khong ghi mat khau vao Excel.
- Hien phan tram tien do, buoc hien tai, so Pass/Fail va anh preview sau moi buoc.
- Dung tien trinh dang chay.
- Luu lich su trong SQLite.
- Sau khi chay: `test-results.xlsx`, `execution.log`, anh tung buoc, `run-video.webm`, `trace.zip`.

## Chay trong IntelliJ IDEA

1. Cai JDK 11.
2. Chon **File → Open** va mo thu muc `TestPilot-Studio`.
3. Cho IntelliJ import Maven va tai dependency.
4. Chay mot lan lenh sau trong Maven tool window de cai Chromium:

   ```text
   exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
   ```

5. Chay class `com.testpilot.TestPilotApplication` hoac Maven goal:

   ```bash
   mvn javafx:run
   ```

## Chay nhu desktop app Java thong thuong

Sau khi build, Maven se copy cac thu vien runtime vao `target/dependency`.

```bash
mvn clean package
java -cp "target/testpilot-studio-0.1.0.jar;target/dependency/*" com.testpilot.Launcher
```

Tren Windows co the chay nhanh:

```bat
scripts\run-desktop.bat
```

## Thu nhanh

1. Mo file `sample-data/TestPilot_Automation_Template.xlsx`.
2. Sua `${BASE_URL}/employees` thanh man hinh danh sach cua ban, hoac tao project co Base URL dung.
3. Sua cac target `testid=search-input`, `role=button,name=Tim kiem` va `testid=result-table` cho khop website.
4. Trong ung dung, tao Project → tao Chuc nang → chon file → **Kiem tra file** → **Chay kiem thu**.

## Bien cau hinh

File `config/application.properties` chua URL va tuy chon khong nhay cam. Tai khoan/mat khau co the nhap cho tung phien chay hoac dat qua bien moi truong:

```text
TESTPILOT_USERNAME=tester@example.com
TESTPILOT_PASSWORD=your-secret
```

Trong Excel chi can dung `${USERNAME}` va `${PASSWORD}`. Khong dua mat khau that vao repository.

## Target locator

Thu tu nen dung:

1. `testid=search-input`
2. `role=button,name=Tim kiem`
3. `label=Tai khoan`
4. `placeholder=Nhap tu khoa`
5. `css=.result-table tbody tr`
6. `xpath=//button[@type='submit']`

Nen yeu cau frontend them `data-testid` cho cac thanh phan quan trong. XPath dai de hong khi giao dien thay doi.

## Cau truc package

```text
com.testpilot
├── config              khoi tao ung dung, SQLite, properties
├── controller          dieu phoi UI va service
├── model
│   ├── entity          Project, Feature, Run, Step
│   ├── enums           ActionType, RunStatus
│   ├── request         DTO dau vao
│   └── response        DTO ket qua
├── repository
│   └── impl            truy cap SQLite
├── service             interface
│   └── impl            implementation
├── ui
│   └── components      JavaFX component tai su dung
└── util                locator va bien testcase
```

## Gioi han cua MVP

- Preview cap nhat sau tung buoc, khong phai video 30 FPS. Khi chay non-headless, cua so Chromium that van hien ra de quan sat lien tuc.
- Video chi hoan tat sau khi browser context dong.
- CAPTCHA va OTP that can co co che bypass/test hook rieng tren moi truong test.
- Chua co AI tu hieu testcase tieng Viet tu do. Thiet ke keyword co dinh on dinh va de truy vet hon; AI co the them o giai doan sau de chuyen mo ta tu nhien thanh `Automation Steps`.
- Windows Credential Manager, lich chay va dashboard web tu xa nam trong roadmap.

Xem them [ARCHITECTURE.md](docs/ARCHITECTURE.md) va [EXCEL_SPEC.md](docs/EXCEL_SPEC.md).
"# tool-auto-testing" 
