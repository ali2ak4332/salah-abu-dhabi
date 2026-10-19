# Official Awqaf Abu Dhabi timetable through 31 December 2027

Use `times_extended.csv` as the Salah app `app/src/main/assets/times.csv` asset. It contains 447 continuous dates, 2026-10-11 through 2027-12-31. The original header and 365 rows are preserved byte-for-byte; 82 official rows are appended.

All 365 rows for 2027 were fetched from the public prayer service used by [Awqaf’s official website](https://www.awqaf.gov.ae/prayer-times). Every monthly response identifies “Abu Dhabi - Abu Dhabi”, official location 36, and “Official Times” with `official: true`. Each original response is in `raw/`.

All 283 days overlapping the app’s existing 2027 data match exactly across Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha (1,698 values, zero mismatches). There are no missing or duplicate dates, invalid times, or prayer ordering errors. Date/weekdays and month/year labels are checked; all day-to-day changes are at most two minutes.

## Files
- `times_extended.csv`: integration-ready complete asset.
- `times_extension_2027.csv`: only the 82 new dates, with CSV header.
- `awqaf_abu_dhabi_2027.csv` and `.json`: full 2027 official normalized data.
- `provenance.json`: endpoints, exact monthly request bodies, field mapping, response SHA-256 hashes and validation results.
- `validate_timetable.py`: offline reproducible validation. Run with Python 3.
- `validation_results.json`: saved validation output.

All times are HH:mm, 24-hour UAE local time (Asia/Dubai, UTC+04:00). Values are copied verbatim from the official API with column-name mapping only. No forecasts, recalculations, or iqamah times are included. Existing 2026 rows are preserved and were not re-fetched. Future official revisions remain possible.
