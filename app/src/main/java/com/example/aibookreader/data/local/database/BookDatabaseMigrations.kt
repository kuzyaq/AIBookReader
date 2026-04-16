package com.example.aibookreader.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName", "MaxLineLength")
object BookDatabaseMigrations {

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=0")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `books_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `author` TEXT NOT NULL,
                    `filePath` TEXT NOT NULL,
                    `coverImage` TEXT,
                    `status` TEXT NOT NULL,
                    `totalPages` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `lastReadAt` INTEGER NOT NULL,
                    `fileSize` INTEGER NOT NULL,
                    `extractedDir` TEXT,
                    `opfBasePath` TEXT,
                    `format` TEXT NOT NULL,
                    `remoteBookId` TEXT,
                    `remoteBookVersion` INTEGER
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `books_new` (
                    `id`, `title`, `author`, `filePath`, `coverImage`, `status`, `totalPages`,
                    `createdAt`, `lastReadAt`, `fileSize`, `extractedDir`, `opfBasePath`,
                    `format`, `remoteBookId`, `remoteBookVersion`
                )
                SELECT
                    `id`, `title`, `author`, `filePath`, `coverImage`, `status`, `totalPages`,
                    `createdAt`, `lastReadAt`, `fileSize`, `extractedDir`, `opfBasePath`,
                    CASE WHEN lower(`filePath`) LIKE '%.pdf' THEN 'PDF' ELSE 'EPUB' END,
                    NULL, NULL
                FROM `books`
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TEMP TABLE IF NOT EXISTS `_migrate_reading_progress` AS
                SELECT
                    `id` AS `bookId`,
                    `locator` AS `locatorJson`,
                    `currentPage` AS `currentPageIndex`,
                    `lastReadAt` AS `lastReadAt`
                FROM `books`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS `reading_progress`")

            db.execSQL("DROP TABLE `books`")
            db.execSQL("ALTER TABLE `books_new` RENAME TO `books`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reading_progress` (
                    `bookId` INTEGER NOT NULL,
                    `locatorJson` TEXT,
                    `currentPageIndex` INTEGER NOT NULL,
                    `lastReadAt` INTEGER NOT NULL,
                    `progressFraction` REAL,
                    `remoteProgressVersion` INTEGER,
                    PRIMARY KEY(`bookId`),
                    FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `reading_progress` (
                    `bookId`, `locatorJson`, `currentPageIndex`, `lastReadAt`, `progressFraction`, `remoteProgressVersion`
                )
                SELECT `bookId`, `locatorJson`, `currentPageIndex`, `lastReadAt`, NULL, NULL
                FROM `_migrate_reading_progress`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS `_migrate_reading_progress`")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_progress_bookId` ON `reading_progress` (`bookId`)")

            db.execSQL("PRAGMA foreign_keys=1")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_ai_retry` (
                    `bookId` INTEGER NOT NULL,
                    `prompt` TEXT NOT NULL,
                    `userMessage` TEXT NOT NULL,
                    `errorMessage` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`bookId`)
                )
                """.trimIndent()
            )
        }
    }
}
