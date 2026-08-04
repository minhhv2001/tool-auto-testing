# Dinh dang Excel

Runner doc sheet co ten chinh xac `Automation Steps`. Dong dau tien la header.

| Cot | Bat buoc | Mo ta |
|---|---:|---|
| TestCaseID | Co | Ma testcase, vi du `TC_SEARCH_01` |
| Step | Co | Thu tu buoc trong testcase |
| Description | Khong | Mo ta cho nguoi doc va log |
| Action | Co | Tu khoa action duoc ho tro |
| Target | Tuy action | Locator cua phan tu |
| Input | Tuy action | Du lieu nhap, URL, phim hoac thoi gian doi |
| Expected | Tuy action | Ket qua mong doi |
| TimeoutMs | Khong | Mac dinh 15000 ms |
| Enabled | Khong | `Yes/No`; mac dinh `Yes` |

## Vi du tim kiem

| TestCaseID | Step | Action | Target | Input | Expected |
|---|---:|---|---|---|---|
| TC_SEARCH_01 | 1 | goto |  | `${BASE_URL}/employees` |  |
| TC_SEARCH_01 | 2 | fill | `testid=search-input` | `Ha Van Minh` |  |
| TC_SEARCH_01 | 3 | click | `role=button,name=Tim kiem` |  |  |
| TC_SEARCH_01 | 4 | wait | `testid=result-table` |  |  |
| TC_SEARCH_01 | 5 | expectRowsContain | `css=[data-testid='result-table'] tbody tr` |  | `Ha Van Minh` |

`expectRowsContain` lay danh sach tat ca locator trung khop, yeu cau danh sach khong rong va tung dong deu chua Expected (khong phan biet hoa thuong).

## Bien

- `${BASE_URL}`: URL cua project dang chon.
- `${USERNAME}`: o tai khoan cua phien chay hoac `TESTPILOT_USERNAME`.
- `${PASSWORD}`: o mat khau cua phien chay hoac `TESTPILOT_PASSWORD`.
- Moi property co tien to `env.` trong `config/application.properties` deu thanh bien Excel. Vi du `env.API_URL=...` tao `${API_URL}`.

Neu file dung mot bien chua duoc cau hinh, step do Fail voi thong bao ro ten bien.
