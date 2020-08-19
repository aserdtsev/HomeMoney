$date = Get-Date -Format "yyyyMMdd"
$filename = "hm-$date.backup"
$env:PGPASSWORD="serdtsev"
pg_dump -f $filename -Fc homemoney
