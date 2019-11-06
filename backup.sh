#!/usr/bin/env bash

filename="hm-$(date +%Y%m%d).backup"
/usr/lib/postgresql/11/bin/pg_dump -p 5433 -Fc homemoney | gzip > "$filename"
echo "$filename"