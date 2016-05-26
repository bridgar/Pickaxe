package me.bridgar;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
	
	public HashMap<UUID,PermissionAttachment> attachments;
	public HashMap<UUID, InetSocketAddress> playerIps;
	public HashMap<UUID, String> playerBytes;
	public HashMap<UUID, String> playerAddresses;
	public HashMap<UUID, ConcurrentLinkedQueue<String>> playerIn;
	public HashMap<UUID, Thread> playerThreads;
	public PlayerListener pl;
	public Permission playerPermission;
	public Random r;
	
	@Override
	public void onEnable() {
		attachments = new HashMap<UUID, PermissionAttachment>();
		playerIps = new HashMap<UUID, InetSocketAddress>();
		playerBytes = new HashMap<UUID, String>();
		playerAddresses = new HashMap<UUID, String>();
		playerIn = new HashMap<UUID, ConcurrentLinkedQueue<String>>();
		playerThreads = new HashMap<UUID, Thread>();
		r = new Random();
		
		pl = new PlayerListener(this, attachments, playerIps);
		PluginManager pm = getServer().getPluginManager();
		playerPermission = new Permission(pl.PERM);
		pm.addPermission(playerPermission);
	}
	
	@Override
	public void onDisable() {
		//TODO save perms list to file
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("fun")) {
			if(args.length != 1) {
				sender.sendMessage("Needs exactly one argument!");
				return false;
			}
			
			Player target = (Bukkit.getServer().getPlayer(args[0]));
			if (target == null) {
				sender.sendMessage(args[0] + " is not online!");
				//add perms to offline player
				this.getConfig().set(args[0], true);
				sender.sendMessage(args[0] + " has been added to the perms list anyway.");
			} else {
				//add perms to online player
				this.getConfig().set(args[0], true);
				
				attachments.get(target.getUniqueId()).setPermission(pl.PERM, true);
				
				pl.createPlayerWorld(target);
			}
			this.saveConfig();
			return true;
		} else if(cmd.getName().equalsIgnoreCase("nofun")) {
			if(args.length != 1) {
				sender.sendMessage("Needs exactly one argument!");
				return false;
			}
			
			Player target = Bukkit.getServer().getPlayer(args[0]);
			if (target == null) {
				sender.sendMessage(args[0] + " is not online!");
				//remove perms from offline player
				this.getConfig().set(args[0], null);
				sender.sendMessage(args[0] + " has been removed from the perms list anyway.");
				
				pl.deleteStringWorld(args[0]);
			} else {
				//remove perms from online player
				this.getConfig().set(args[0], null);
				
				attachments.get(target.getUniqueId()).setPermission(pl.PERM, false);
				
				pl.deletePlayerWorld(target);
			}
			
			this.saveConfig();
			return true;
		} else if(cmd.getName().equalsIgnoreCase("j")) {
			System.out.println("j");
			String value = "";
			
			for(int i = 0; i < args.length; i++) {
				value += args[i] + " ";
			}
			
			value = value.substring(0, value.length()-1);
			value = value.replace("\\ n", "\n");
			value = value.replace("\\ r", "\r");
			value = value.replace("\\ ", " ");
			
			UUID id = Bukkit.getServer().getPlayer(sender.getName()).getUniqueId();
			if(playerBytes.containsKey(id)) {
				playerBytes.replace(id, playerBytes.get(id) + value);
			} else {
				playerBytes.put(id, value);
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("k")) {
			System.out.println("k");
			String value = "";
			
			for(int i = 0; i < args.length; i++) {
				value += args[i] + " ";
			}
			
			value = value.substring(0, value.length()-2);
			
			UUID id = Bukkit.getServer().getPlayer(sender.getName()).getUniqueId();
			if(playerAddresses.containsKey(id)) {
				playerAddresses.replace(id, value);
			} else {
				playerAddresses.put(id, value);
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("l")) {
			System.out.println("l");
			UUID id = Bukkit.getServer().getPlayer(sender.getName()).getUniqueId();
			if(!(playerBytes.containsKey(id) && playerAddresses.containsKey(id))) {
				sender.sendMessage("j and k before l");
			}
			sender.sendMessage(playerBytes.get(id));
			
			if(!(playerIn.containsKey(id))) {
				playerIn.put(id, new ConcurrentLinkedQueue<String>());
				Thread t = new Thread(new ProxyServer(sender, playerIn.get(id)));
				t.start();
				playerThreads.put(id, t);
			}
			
			playerIn.get(id).add(playerBytes.get(id));
			playerBytes.replace(id, "");
		}
		
		return false;
	}
	
	public void playerLoggedOff(UUID id) {
		playerBytes.remove(id);
		playerAddresses.remove(id);
		playerIn.remove(id);
		playerThreads.get(id).interrupt();
		playerThreads.remove(id);
	}
	
}
