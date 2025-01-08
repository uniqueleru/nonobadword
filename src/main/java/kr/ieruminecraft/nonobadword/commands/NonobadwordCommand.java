package kr.ieruminecraft.nonobadword.commands;

import kr.ieruminecraft.nonobadword.Nonobadword;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class NonobadwordCommand implements CommandExecutor, TabCompleter {

    private final Nonobadword plugin;

    public NonobadwordCommand(Nonobadword plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nonobadword.admin")) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable":
                plugin.getConfig().set("enabled", true);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "욕설 감지 시스템이 활성화되었습니다.");
                break;

            case "disable":
                plugin.getConfig().set("enabled", false);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.YELLOW + "욕설 감지 시스템이 비활성화되었습니다.");
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.loadConfigValues();
                sender.sendMessage(ChatColor.GREEN + "설정 파일이 리로드되었습니다.");
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("enable");
            completions.add("disable");
            completions.add("reload");
            return completions;
        }

        return completions;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Nonobadword 명령어 ===");
        sender.sendMessage(ChatColor.YELLOW + "/nbw enable " + ChatColor.WHITE + "- 욕설 감지 시스템 활성화");
        sender.sendMessage(ChatColor.YELLOW + "/nbw disable " + ChatColor.WHITE + "- 욕설 감지 시스템 비활성화");
        sender.sendMessage(ChatColor.YELLOW + "/nbw reload " + ChatColor.WHITE + "- 설정 파일 리로드");
    }

}