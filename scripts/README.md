# Curtain Production Backup Scripts

Bu klasör MySQL veritabanı için otomatik yedekleme scriptlerini içerir.

## Scriptler

- **backup.sh** - Günlük otomatik yedekleme scripti
- **restore.sh** - Veritabanı geri yükleme scripti  
- **manual-backup.sh** - Manuel yedekleme scripti

## Kurulum

Detaylı kurulum talimatları için ana dokümantasyona bakın:
- Backup Setup Guide (artifacts klasöründe)

## Hızlı Başlangıç

```bash
# Sunucuda
chmod +x /opt/curtain/scripts/*.sh
/opt/curtain/scripts/manual-backup.sh
```
