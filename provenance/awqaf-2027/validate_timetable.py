"""Validate stored official Awqaf data and the Salah app extension (no network)."""
import calendar, csv, datetime as dt, hashlib, json, re
from pathlib import Path
root=Path(__file__).resolve().parent
keys={'date':'date','fajr':'fajer','sunrise':'shuroq','dhuhr':'dhohur','asr':'aser','maghrib':'maghreb','isha':'isha'}
rows=[]
for month in range(1,13):
    payload=json.loads((root/'raw'/f'2027-{month:02d}.json').read_text())
    assert not payload['errors']
    result=payload['result']; meta=result['meta_data']
    assert meta['method']['official'] is True
    assert meta['method']['name']=='Official Times'
    assert str(meta['location']['official_id'])=='36'
    assert meta['location']['city']=='Abu Dhabi - Abu Dhabi'
    raw=result['prayer_times']
    assert len(raw)==calendar.monthrange(2027,month)[1]
    for item in raw:
        date=dt.date.fromisoformat(item['date'])
        assert (date.year,date.month)==(2027,month)
        assert date.strftime('%A')==item['day']
        row={k:item[v] for k,v in keys.items()}
        for k in list(keys)[1:]: assert re.fullmatch(r'\d{2}:\d{2}',row[k])
        values=[dt.time.fromisoformat(row[k]) for k in list(keys)[1:]]
        assert all(a<b for a,b in zip(values,values[1:]))
        mins=[t.hour*60+t.minute for t in values]
        assert 180<=mins[0]<=390 and 300<=mins[1]<=480 and 690<=mins[2]<=810
        assert 840<=mins[3]<=1020 and 1020<=mins[4]<=1200 and 1080<=mins[5]<=1320
        rows.append(row)
assert len(rows)==365 and len({r['date'] for r in rows})==365
assert [r['date'] for r in rows]==[(dt.date(2027,1,1)+dt.timedelta(days=i)).isoformat() for i in range(365)]
assert json.loads((root/'awqaf_abu_dhabi_2027.json').read_text())==rows
assert list(csv.DictReader((root/'awqaf_abu_dhabi_2027.csv').open()))==rows
merged=list(csv.DictReader((root/'times_extended.csv').open()))
assert len(merged)==447 and len({r['date'] for r in merged})==447
assert [r['date'] for r in merged]==[(dt.date(2026,10,11)+dt.timedelta(days=i)).isoformat() for i in range(447)]
assert merged[-365:]==rows
extension=list(csv.DictReader((root/'times_extension_2027.csv').open()))
assert merged[-82:]==extension
baseline=b''.join((root/'times_extended.csv').read_bytes().splitlines(keepends=True)[:366])
assert hashlib.sha256(baseline).hexdigest()=='eaeb18f86f854ae11ed6f34c8415402ff124e97c24b7f15707804a39befaa5ea'
max_daily_change={}
for k in list(keys)[1:]:
    mins=[int(r[k][:2])*60+int(r[k][3:]) for r in rows]
    max_daily_change[k]=max(abs(a-b) for a,b in zip(mins,mins[1:]))
    assert max_daily_change[k]<=2
print(json.dumps({'result':'PASS','official_2027_rows':len(rows),'extended_app_rows':len(merged),'extension_rows':len(extension),'existing_283_overlap_rows_exact_match':True,'original_365_rows_byte_identical':True,'maximum_day_to_day_change_minutes':max_daily_change},indent=2))
