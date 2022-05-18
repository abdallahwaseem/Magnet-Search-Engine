DROP DATABASE IF EXISTS MagnetSG;

CREATE DATABASE MagnetSG;

USE MagnetSG;
DROP TABLE IF EXISTS CrawlerData;
DROP TABLE IF EXISTS UrlsToBeCrawled;
DROP TABLE IF EXISTS SearchData;



CREATE TABLE CrawlerData (
Urls nvarchar(300) COLLATE Arabic_CI_AI_KS_WS NOT NULL  PRIMARY KEY,
CompactPages nvarchar(300) COLLATE Arabic_CI_AI_KS_WS,
Filename nvarchar(55),
UNIQUE (CompactPages)
);

CREATE TABLE UrlsToBeCrawled (
Urls nvarchar(300) COLLATE Arabic_CI_AI_KS_WS NOT NULL PRIMARY KEY
);

CREATE TABLE SearchData (
Query nvarchar(300) COLLATE Arabic_CI_AI_KS_WS NOT NULL PRIMARY KEY
);