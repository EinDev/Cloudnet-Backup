# CloudNet-Backup

This is based on [Cloudnet-Backup](https://github.com/MarkusTieger/Cloudnet-Backup) from MarkusTieger.

> [!WARNING]
> This module is in an experimental stage.
> While it works, it is untested and a major rewrite from the upstream module.
> Also, **it lacks the ability to delete backups**.
> Deleting the DB rows and then running a GC cycle shoulddo the job.
> Use at your own risk!

## Key differences to the upstream version

- Support for CloudNet v4
  - The Module now uses the same database CloudNet uses, instead of having its own configuration
- Added [BlobStorage](src/main/java/dev/ein/cloudnet/module/backup/data/BlobStorage.java), which means a lot of things:
  - Added deduplication, based on SHA265 hashes of the files being backed up
  - Detached the Blob storage from the metadata storage, which allows for S3 storage and similar in the future
- Switched build system to gradle
- Migrated Backup metadata to a single-record format. This should reduce database load.

## Storage schema

This module has two main storages:
- The Database (configured in CloudNet) for storing backup and file metadata
- The BlobStorage (configurable (soon) in the module) for storing the file content

The Database stores the hashes of each backed up file, while the BlobStorage holds the file content,
indexed by the hash of the content.
This way, we only need to back up files once and (if the file did not change) we can even repair damaged backups!

## TODO (feel free to contribute)
- (Beginner friendly) Add Node information to BackupInfo (Allows for multi-node cluster backups)
  - This should allow for multi-node clusters, but requires the same database to be used
  - (optional) Add a note to the user  
- (Beginner friendly) Delete folders from ".files" during GC cycle, if they are empty
- (Beginner friendly) Make the folder ".files" configurable
- (Beginner friendly) Replace ``System.out`` calls with proper logging
- Calculate the deduplication factor during GC
- Add sanity checking backups
  - Check if files in each backup are present in BlobStorage
  - Check if checksums of files in BlobStorage match filenames
  - If checksums mismatch, mark blob and corresponding backup as "damaged"
    - This allows us to repair the blob, if we try to back up the same blob again
    - We shall not delete anything, unless the user asks us to
  - Check for files other than blobs (warn the user, if possible)
- Add Tests (this is a big one)
  - GC cycles
  - Restoring backups
  - Deduplication efficiency tracking
    - Generate sample data
    - Make multiple backups
    - Show the deduplication factor
- Add periodic progress messages
- Make backup processes asynchronous
  - Ensure only one backup runs at a time!
- Add the ability to use shared storage backends
  - S3
  - FTP
- Add task scheduling (Backups, GC & Sanity-Checks)
- Add a local agent to allow for `save-all` before backup (inside the service)
- Add the ability to restore a single service ("preview" backup)
- Optimize deduplication
  - Don't compress folders, compress individual files instead
  - use more deterministic compression (just an idea, maybe this is not even an issue)
