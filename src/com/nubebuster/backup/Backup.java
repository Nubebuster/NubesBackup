package com.nubebuster.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class Backup extends JavaPlugin {

	private static Backup inst;
	private static FileConfiguration config;

	private long lastBackup, backupdelay = -1, maxAge = -1;

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		inst = this;
		configs();
		if (config.isSet("lastbackup"))
			lastBackup = config.getLong("lastbackup");
		if (config.isSet("delay")) {
			String del = config.getString("delay");
			String[] data = del.split(":");
			if (data.length == 4) {
				try {
					long delay = 0;
					delay += Integer.parseInt(data[0]) * 86400000;
					delay += Integer.parseInt(data[1]) * 3600000;
					delay += Integer.parseInt(data[2]) * 60000;
					delay += Integer.parseInt(data[3]) * 1000;
					backupdelay = delay;
				} catch (NumberFormatException e) {
					System.err.println("'delay' in the config is not in the right format! (dd:HH:mm:ss)");
				}
			} else {
				if (!del.equals("disabled")) {
					backupdelay = 86400000;
					System.out.println("'delay' is not correctly set in the config, setting to 24 hours...");
				}
			}
		}
		if (config.isSet("deleteifoldarthan")) {
			String del = config.getString("deleteifoldarthan");
			String[] data = del.split(":");
			if (data.length == 4) {
				try {
					long delay = 0;
					delay += Integer.parseInt(data[0]) * 86400000;
					delay += Integer.parseInt(data[1]) * 3600000;
					delay += Integer.parseInt(data[2]) * 60000;
					delay += Integer.parseInt(data[3]) * 1000;
					backupdelay = delay;
				} catch (NumberFormatException e) {
					System.err.println("'deleteifoldarthan' in the config is not in the right format! (dd:HH:mm:ss)");
				}
			} else if (!del.equals("disabled")) {
				System.err.println("'deleteifoldarthan' in the config is not in the right format! (dd:HH:mm:ss)");
			}
		}

		if (backupdelay != -1) {
			if (lastBackup - System.currentTimeMillis() > backupdelay)
				Bukkit.getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
					public void run() {
						try {
							backup();
							System.out.println("Finished backup.");
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("An error ocurred during the backup!");
						}
					}
				});
			long next = (backupdelay - (System.currentTimeMillis() - lastBackup)) / 50;
			Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
				public void run() {
					try {
						backup();
						System.out.println("Finished backup.");
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("An error ocurred during the backup!");
					}
				}
			}, next > 0 ? next : 40, backupdelay / 50);
		}
		commands();
	}

	private void backup() throws Exception {
		lastBackup = System.currentTimeMillis();
		config.set("lastbackup", lastBackup);
		saveConfig();
		System.out.println("Saving worlds before backup...");
		for (World w : Bukkit.getWorlds())
			w.save();
		System.out.println("Running backup...");
		String date = new SimpleDateFormat("yyyy-MM-dd HH;mm;ss").format(new Date());
		File backupFolder = new File("backup");
		if (!backupFolder.exists())
			backupFolder.mkdir();
		FileOutputStream fos = new FileOutputStream(new File(backupFolder, "backup " + date + ".zip"));
		ZipOutputStream zos = new ZipOutputStream(fos);

		List<File> files = new ArrayList<File>();

		// TODO add files
		for (String s : config.getStringList("backuplist")) {
			File f = new File(s);
			if (!f.exists())
				continue;
			if (f.isDirectory()) {
				getFilesFromDirectory(f, files);
			} else
				files.add(f);
		}

		for (File f : files) {
			if (f.length() == 0)
				continue;
			FileInputStream fis = new FileInputStream(f);
			ZipEntry zipEntry = new ZipEntry(f.getPath());
			zos.putNextEntry(zipEntry);

			byte[] bytes = new byte[(int) f.length()];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zos.write(bytes, 0, length);
			}

			zos.closeEntry();
			fis.close();
		}
		zos.close();
		fos.close();
		if (maxAge > 0) {
			System.out.println("Removing old backups");
			for (File f : backupFolder.listFiles())
				if (System.currentTimeMillis() - f.lastModified() > maxAge)
					f.delete();
		}
	}

	private List<File> getFilesFromDirectory(File directory, List<File> to) {
		for (File f : directory.listFiles()) {
			if (f.isDirectory())
				getFilesFromDirectory(f, to);
			else
				to.add(f);
		}
		return to;
	}

	private void commands() {
		getCommand("backup").setExecutor(new CommandExecutor() {
			@SuppressWarnings("deprecation")
			@Override
			public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] arg3) {
				if (!sender.hasPermission("nubesbackup.backup")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to perform this command!");
					return false;
				}
				sender.sendMessage(ChatColor.GREEN + "Starting backup...");
				Bukkit.getScheduler().scheduleAsyncDelayedTask(inst, new Runnable() {
					public void run() {
						try {
							backup();
							sender.sendMessage(ChatColor.GREEN + "Finished backup");
						} catch (Exception e) {
							e.printStackTrace();
							sender.sendMessage(ChatColor.RED + "An error ocurred during the backup!");
						}
					}
				});
				return false;
			}
		});

		getCommand("backupreload").setExecutor(new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] arg3) {
				if (!sender.hasPermission("nubesbackup.reload")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to perform this command!");
					return false;
				}
				reloadConfig();
				sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
				return false;
			}
		});
	}

	private void configs() {
		saveDefaultConfig();
		config = getConfig();
		if (config.getBoolean("rewriteconfig")) {
			File file = new File(getDataFolder() + File.separator + "config.yml");
			file.delete();
			saveDefaultConfig();
		}
	}
}
