package com.nokhoon.deathban;

import java.util.Date;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.BanList.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PluginMain extends JavaPlugin implements Listener {
	private int timeInitial, timeAdditional;
	
	public static final String VALUE_FORMAT = "값은 음이 아닌 정수로, 단위는 초입니다.";
	public static final String PLAYER_FORMAT = "플레이어는 원하는 플레이어의 이름입니다.";
	
	private int getPlayerRespawnTime(Player player) {
		return getConfig().getInt("players." + player.getUniqueId().toString() + ".time", 0);
	}
	private void setPlayerRespawnTime(Player player, int time) {
		getConfig().set("players." + player.getUniqueId().toString() + ".time", time);
		saveConfig();
	}
	private Component playerMessageHeader(Player player) {
		return Component.text(player.getName()).color(NamedTextColor.YELLOW).hoverEvent(player.asHoverEvent())
				.append(Component.text(" 님이 ", NamedTextColor.WHITE));
	}
	private Component respawnTimeGetMessage(int seconds) {
		if(seconds < 0) return null;
		return Component.text(" " + seconds, NamedTextColor.GRAY)
				.append(Component.text("초입니다.", NamedTextColor.GREEN));
	}
	private Component respawnTimeSetMessage(int seconds) {
		if(seconds < 0) return null;
		return Component.text(" " + seconds, NamedTextColor.GRAY)
				.append(Component.text("초로 설정되었습니다.", NamedTextColor.GREEN));
	}
	private Component respawnTimeChangeMessage(int changeInSeconds) {
		if(changeInSeconds > 0) return Component.text(" " + changeInSeconds, NamedTextColor.GRAY)
				.append(Component.text("초 증가했습니다.", NamedTextColor.RED));
		else if(changeInSeconds < 0) return Component.text(" " + (-changeInSeconds), NamedTextColor.GRAY)
				.append(Component.text("초 감소했습니다.", NamedTextColor.GREEN));
		else return null;
	}
	private void informEnterInteger(Audience audience) {
		audience.sendMessage(PluginConstants.error("음이 아닌 정수를 입력해 주세요."));
	}
	private void informEnterPlayer(Audience audience) {
		audience.sendMessage(PluginConstants.error("플레이어의 이름을 입력해 주세요."));
	}
	private void informUnknownPlayer(Audience audience) {
		audience.sendMessage(PluginConstants.error("해당 이름을 가진 플레이어가 존재하지 않습니다."));
	}
	private void informPlayerRespawnTime(Audience audience, String player, int time) {
		audience.sendMessage(PluginConstants.HEADER_INFO
				.append(Component.text(player, NamedTextColor.GRAY)
				.append(Component.text(" 님의 고유 리스폰 대기 시간은", NamedTextColor.GREEN)
				.append(respawnTimeGetMessage(time)))));
	}
	private void broadcastRespawnTimeChange(int changeInSeconds) {
		if(changeInSeconds == 0) return;
		((Audience) getServer()).sendMessage(PluginConstants.HEADER_INFO
				.append(Component.text("리스폰 대기 시간이")
				.append(respawnTimeChangeMessage(changeInSeconds))));
	}
	
	@Override
	public void onEnable() {
		timeInitial = getConfig().getInt("default", 10);
		timeAdditional = getConfig().getInt("additional", 0);
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {
		getConfig().set("default", timeInitial);
		getConfig().set("additional", timeAdditional);
		saveConfig();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Audience audience = (Audience) player;
		
		if(timeInitial > 0 && player.isDead()) event.joinMessage(playerMessageHeader(player)
				.append(Component.text("악몽에서 깨어났습니다. 괜찮아요?", NamedTextColor.WHITE)));
		else event.joinMessage(playerMessageHeader(player)
				.append(Component.text("서버에 접속했습니다.", NamedTextColor.WHITE)));
		
		if(timeInitial > 0) {
			audience.sendMessage(PluginConstants.warning("조심하세요! 죽으면 " + (timeInitial + getPlayerRespawnTime(player)) + "초 동안 서버에 접속할 수 없습니다."));
			if(timeAdditional > 0) audience.sendMessage(Component.text("리스폰 대기 시간은 죽을 때마다 " + timeAdditional + "초씩 늘어납니다.", NamedTextColor.YELLOW));
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if(timeInitial > 0 && player.isDead()) event.quitMessage(playerMessageHeader(player)
				.append(Component.text("잠들었습니다.", NamedTextColor.WHITE)));
		else event.quitMessage(playerMessageHeader(player)
				.append(Component.text("서버에서 나갔습니다.", NamedTextColor.WHITE)));
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if(timeInitial == 0) return;
		Player player = event.getEntity();
		String name = player.getName();
		String id = player.getUniqueId().toString();
		Component deathMessage = event.deathMessage() == null ?
				Component.text("죽었습니다!", NamedTextColor.RED) : event.deathMessage();
		int time = timeInitial + getPlayerRespawnTime(player);
		int add = timeAdditional;
		int nextTime = time - timeInitial + add;
		Date date = new Date(System.currentTimeMillis() + time * 1000L);
		
		getServer().getScheduler().runTask(this, new Runnable() {
			public void run() {
				String stringDeathMessage = event.getDeathMessage();
				String banMessage = ChatColor.RED + "죽었습니다!";
				if(stringDeathMessage != null) banMessage += (" " + ChatColor.WHITE + stringDeathMessage);
				player.kick(deathMessage
						.append(Component.text(" " + time, NamedTextColor.GRAY)
						.append(Component.text("초 뒤에 접속할 수 있습니다.", NamedTextColor.RED))));
				getServer().getBanList(Type.NAME).addBan(name, banMessage, date, getDescription().getFullName());
				getConfig().set("players." + id + ".name", name);
				if(add > 0) getConfig().set("players." + id + ".time", nextTime);
				saveConfig();
			}
		});
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("respawn")) {
			if(args.length == 0) return false;
			Audience audience = (Audience) sender;
			switch(args[0].toLowerCase()) {
			case "additional":
				if(args.length == 1) audience.sendMessage(PluginConstants.info("현재 리스폰 대기 시간 증가량은")
						.append(respawnTimeGetMessage(timeAdditional)));
				else if(args.length > 2) {
					audience.sendMessage(PluginConstants.error("명령어 사용법: /respawn additional [값]"));
					audience.sendMessage(PluginConstants.info(VALUE_FORMAT));
				}
				else if(!sender.isOp()) audience.sendMessage(PluginConstants.NO_PERMISSION);
				else try {
					timeAdditional = Integer.parseInt(args[1]);
					audience.sendMessage(PluginConstants.info("리스폰 대기 시간 증가량이")
							.append(respawnTimeSetMessage(timeAdditional)));
				} catch (NumberFormatException e) {
					informEnterInteger(sender);
				}
				return true;
			case "default":
				if(args.length == 1) audience.sendMessage(PluginConstants.info("현재 기본 리스폰 대기 시간은")
						.append(respawnTimeGetMessage(timeInitial)));
				else if(args.length > 2) {
					audience.sendMessage(PluginConstants.error("명령어 사용법: /respawn default [값]"));
					audience.sendMessage(PluginConstants.info(VALUE_FORMAT));
				}
				else if(!sender.isOp()) audience.sendMessage(PluginConstants.NO_PERMISSION);
				else try {
					int previous = timeInitial;
					timeInitial = Integer.parseInt(args[1]);
					audience.sendMessage(PluginConstants.info("기본 리스폰 대기 시간이")
							.append(respawnTimeSetMessage(timeInitial)));
					broadcastRespawnTimeChange(timeInitial - previous);
				} catch (NumberFormatException e) {
					informEnterInteger(sender);
				}
				return true;
			case "get":
				if(args.length > 2) {
					audience.sendMessage(PluginConstants.error("명령어 사용법: /respawn get [플레이어]"));
					audience.sendMessage(PluginConstants.info(PLAYER_FORMAT));
				}
				else {
					if(args.length == 1) {
						if(sender instanceof Player) {
							Player player = (Player) sender;
							int value = getPlayerRespawnTime(player);
							audience.sendMessage(PluginConstants.info("당신의 고유 리스폰 대기 시간은")
									.append(respawnTimeGetMessage(value)));
						}
						else informEnterPlayer(sender);
					}
					else if(!sender.isOp()) audience.sendMessage(PluginConstants.NO_PERMISSION);
					else {
						for(Player player : getServer().getOnlinePlayers()) {
							if(player.getName().equals(args[1])) {
								int value = getPlayerRespawnTime(player);
								informPlayerRespawnTime(sender, args[1], value);
								return true;
							}
						}
						UUID id = getServer().getPlayerUniqueId(args[1]);
						if(id != null) {
							int value = getConfig().getInt("players." + id.toString() + ".time", 0);
							informPlayerRespawnTime(sender, args[1], value);
						}
						else informUnknownPlayer(sender);
					}
				}
				return true;
			case "set":
				if(args.length == 1 || args.length > 3) {
					audience.sendMessage(PluginConstants.error("명령어 사용법: /respawn set (값) [플레이어]"));
					audience.sendMessage(PluginConstants.info(PLAYER_FORMAT + ' ' + VALUE_FORMAT));
				}
				else if(!sender.isOp()) audience.sendMessage(PluginConstants.NO_PERMISSION);
				else try {
					int input = Integer.parseInt(args[1]);
					if(input < 0) {
						informEnterInteger(sender);
						return true;
					}
					if(args.length == 2) {
						if(sender instanceof Player) {
							Player player = (Player) sender;
							int previous = getPlayerRespawnTime(player);
							setPlayerRespawnTime(player, input);
							audience.sendMessage(PluginConstants.info("당신의 고유 리스폰 대기 시간이")
									.append(respawnTimeSetMessage(input)));
							audience.sendMessage(PluginConstants.info("리스폰 대기 시간이")
									.append(respawnTimeChangeMessage(input - previous)));
						}
						else informEnterPlayer(sender);
					}
					else {
						for(Player player : getServer().getOnlinePlayers()) {
							if(player.getName().equals(args[2])) {
								int previous = getPlayerRespawnTime(player);
								setPlayerRespawnTime(player, input);
								Audience p = (Audience) player;
								p.sendMessage(PluginConstants.info("당신의 고유 리스폰 대기 시간이")
										.append(respawnTimeSetMessage(input)));
								p.sendMessage(PluginConstants.info("리스폰 대기 시간이")
										.append(respawnTimeChangeMessage(input - previous)));
								return true;
							}
						}
						UUID id = getServer().getPlayerUniqueId(args[2]);
						if(id != null) {
							getConfig().set("players." + id.toString() + ".time", input);
							saveConfig();
							audience.sendMessage(PluginConstants.HEADER_INFO
									.append(Component.text(args[2], NamedTextColor.GRAY)
									.append(Component.text(" 님의 고유 리스폰 대기 시간이", NamedTextColor.GREEN)
									.append(respawnTimeSetMessage(input)))));
						}
						else informUnknownPlayer(sender);
					}
				} catch (NumberFormatException e) {
					informEnterInteger(sender);
				}
				return true;
			}
		}
		return false;
	}
}
