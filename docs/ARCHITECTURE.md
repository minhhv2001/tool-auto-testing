# Kien truc AUTO TESTING IMD

## Vi sao chon desktop

Desktop la lua chon phu hop cho ban dau vi runner can doc file Excel tren may, mo browser co giao dien, luu video/anh/log va cho phep nguoi dung mo truc tiep artifact. Neu lam web ngay tu dau, van phai cai them mot local agent tren may tester hoac dua credential/file len server.

Huong phat trien de nghi:

1. Desktop local-first cho MVP va tester noi bo.
2. Tach `runner-core` thanh module rieng khi luong action on dinh.
3. Them REST/WebSocket service neu can chay runner tren nhieu may.
4. Xay web dashboard chi de quan ly lich, xem bao cao va phan quyen.

## Luong thuc thi

```text
Excel → ExcelService → TestStep[] → TestRunnerService → Playwright/Chromium
                                           ↓
SQLite ← progress/run history ← event ← screenshot/video/trace/report
```

## Nguyen tac thiet ke

- UI khong thao tac truc tieap voi Playwright hay JDBC.
- Service co interface va implementation tach rieng.
- DTO request/response khong le thuoc JavaFX.
- Moi tien trinh co thu muc artifact bat bien theo Run ID.
- Secret duoc resolve luc chay va khong ghi vao report.
- Cancellation co hieu luc tai ranh gioi giua cac step.
- Mot step loi khong lam mat ket qua cac step da chay.

## Artifact cua mot run

```text
outputs/<run-id>/
├── screenshots/
│   └── 0001-TC_SEARCH_01-01.png
├── test-results.xlsx
├── execution.log
├── run-video.webm
└── trace.zip
```

## Roadmap de thanh san pham hoan chinh

- Phase 1: MVP local (hien tai).
- Phase 2: editor testcase trong app, duplicate/versioning, tag Smoke/Regression, retry rieng testcase loi.
- Phase 3: Windows Credential Manager, environment profiles Dev/UAT/Staging, scheduler va notification.
- Phase 4: API testing, network assertions, database checks, visual regression va accessibility.
- Phase 5: distributed runner, web dashboard, RBAC, CI/CD va AI chuyen testcase tieng Viet thanh step co cau truc.
