@echo off
if "%1"=="" goto bad

:good

rem Do processing here
docker exec -it broker kafka-topics --create --topic %1 --bootstrap-server broker:9092  --replication-factor 1 --partitions 1
goto end

:bad

rem Do error handling here
ECHO please enter a valid topic name

:end
