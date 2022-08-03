# SimpleBackup
A simple backup plugin for spigot

https://www.spigotmc.org/resources/nubesbackup.27001/

This plugin backups your server files and supports mysql backup through running os commands.
You can put a list of directories which you want to be backed up and file extension exclusions in the config file.
You can enable automatic backups at a set time and when old backups are removed.
Supports command line execution. The server may need to be started with sufficient permissions in order to make mysql dump work.

To Install this:
download this plugin and put it in your plugins folder
reload the server
open the config.yml in /plugins/NubesBackup
change the delays and add which folders need to be included in backups
save the file
execute /backupreload
You are now good to go!

Commands:
- /backup
- /backupreload - to reload the config

Permissions:
For /backup
- nubesbackup.backup
For /backupreload
- nubesbackup.reload
