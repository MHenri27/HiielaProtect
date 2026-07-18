package ee.henri.hiielaprotect.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import ee.henri.hiielaprotect.ConfigManager;
import ee.henri.hiielaprotect.DatabaseManager;
import ee.henri.hiielaprotect.HiielaProtect;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class KaitseCommand implements CommandExecutor, TabCompleter {

    private final HiielaProtect plugin;
    private final ConfigManager config;
    private final DatabaseManager db;

    public KaitseCommand(HiielaProtect plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(config.getMessage("help_message"));
            return true;
        }

        String sub = args[0].toLowerCase();
        
        if (sub.equalsIgnoreCase(config.getSubcommandName("create"))) {
            handleCreate(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("remove"))) {
            handleRemove(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("move"))) {
            handleMove(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("addowner"))) {
            handleAddOwner(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("removeowner"))) {
            handleRemoveOwner(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("addmember"))) {
            handleAddMember(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("removemember"))) {
            handleRemoveMember(sender, args);
        } else if (sub.equalsIgnoreCase(config.getSubcommandName("reload"))) {
            handleReload(sender);
        } else {
            sender.sendMessage(config.getMessage("help_message"));
        }
        
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hiielaprotect.admin")) {
            sender.sendMessage(config.getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.getMessage("usage_create"));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("player_only"));
            return;
        }
        Player p = (Player) sender;
        String targetName = args[1];
        
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(p));
        Region selection;
        try {
            selection = session.getSelection(BukkitAdapter.adapt(p.getWorld()));
        } catch (IncompleteRegionException e) {
            p.sendMessage(config.getMessage("no_selection"));
            return;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(p.getWorld()));
        
        if (regions == null) return;
        
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        
        boolean expandVert = true;
        if (args.length >= 3 && args[2].equalsIgnoreCase("no")) {
            expandVert = false;
        }
        
        if (expandVert) {
            min = BlockVector3.at(min.x(), p.getWorld().getMinHeight(), min.z());
            max = BlockVector3.at(max.x(), p.getWorld().getMaxHeight() - 1, max.z());
        }

        ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion("temp", min, max);
        ApplicableRegionSet overlaps = regions.getApplicableRegions(newRegion);
        
        if (overlaps.size() > 0) {
            p.sendMessage(config.getMessage("region_overlap"));
            return;
        }
        
        int nextNum = db.getNextRegionNumber(targetName);
        String regionName = targetName.toLowerCase() + "_" + nextNum;
        
        ProtectedCuboidRegion finalRegion = new ProtectedCuboidRegion(regionName, min, max);
        
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        for (String cmd : config.getCommandsBeforeCreate()) {
            String toRun = cmd.replace("%region_name%", regionName)
                    .replace("%player_name%", targetName)
                    .replace("%command_runner%", p.getName())
                    .replace("%region_number%", String.valueOf(nextNum))
                    .replace("%world%", p.getWorld().getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun);
        }
        
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(target.getUniqueId());
        finalRegion.setOwners(owners);
        
        regions.addRegion(finalRegion);
        
        db.saveRegion(target.getUniqueId().toString(), targetName, regionName);
        
        p.sendMessage(config.getMessage("region_created", targetName, regionName, p.getName()));

        for (String cmd : config.getCommandsOnCreate()) {
            String toRun = cmd.replace("%region_name%", regionName)
                              .replace("%player_name%", targetName)
                              .replace("%command_runner%", p.getName())
                              .replace("%region_number%", String.valueOf(nextNum))
                              .replace("%world%", p.getWorld().getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun);
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hiielaprotect.admin")) {
            sender.sendMessage(config.getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.getMessage("usage_remove"));
            return;
        }
        
        String arg = args[1];
        String regionToRemove = null;
        Player p = (sender instanceof Player) ? (Player) sender : null;
        
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        
        if (arg.equalsIgnoreCase("@here") && p != null) {
            RegionManager regions = container.get(BukkitAdapter.adapt(p.getWorld()));
            if (regions != null) {
                BlockVector3 loc = BukkitAdapter.asBlockVector(p.getLocation());
                ApplicableRegionSet set = regions.getApplicableRegions(loc);
                for (ProtectedRegion pr : set) {
                    if (!pr.getId().equalsIgnoreCase("__global__")) {
                        regionToRemove = pr.getId();
                        break;
                    }
                }
            }
        } else if (arg.startsWith("#")) {
            regionToRemove = arg.substring(1);
        } else {
            regionToRemove = db.getLatestRegion(arg);
        }
        
        if (regionToRemove == null) {
            sender.sendMessage(config.getMessage("region_not_found"));
            return;
        }
        
        boolean removedFromWG = false;
        if (p != null) {
            RegionManager regions = container.get(BukkitAdapter.adapt(p.getWorld()));
            if (regions != null && regions.hasRegion(regionToRemove)) {
                regions.removeRegion(regionToRemove);
                removedFromWG = true;
            }
        } else {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                RegionManager rm = container.get(BukkitAdapter.adapt(w));
                if (rm != null && rm.hasRegion(regionToRemove)) {
                    rm.removeRegion(regionToRemove);
                    removedFromWG = true;
                    break;
                }
            }
        }

        if(!removedFromWG){
            sender.sendMessage(config.getMessage("region_not_found"));
            return;
        }
        
        db.deleteRegion(regionToRemove);
        
        sender.sendMessage(config.getMessage("region_removed", "", regionToRemove, sender.getName()));
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hiielaprotect.admin")) {
            sender.sendMessage(config.getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.getMessage("usage_move"));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("player_only"));
            return;
        }
        Player p = (Player) sender;
        String arg = args[1];
        String regionName = arg.startsWith("#") ? arg.substring(1) : db.getLatestRegion(arg);
        
        if (regionName == null) {
            p.sendMessage(config.getMessage("region_not_found"));
            return;
        }
        
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(p));
        Region selection;
        try {
            selection = session.getSelection(BukkitAdapter.adapt(p.getWorld()));
        } catch (IncompleteRegionException e) {
            p.sendMessage(config.getMessage("no_selection"));
            return;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(p.getWorld()));
        if (regions == null) return;

        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();

        boolean expandVert = true;
        if (args.length >= 3 && args[2].equalsIgnoreCase("no")) {
            expandVert = false;
        }

        if (expandVert) {
            min = BlockVector3.at(min.x(), p.getWorld().getMinHeight(), min.z());
            max = BlockVector3.at(max.x(), p.getWorld().getMaxHeight() - 1, max.z());
        }
        
        ProtectedRegion oldRegion = regions.getRegion(regionName);
        if (oldRegion == null) {
            p.sendMessage(config.getMessage("region_not_found"));
            return;
        }
        
        ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion("temp_move", min, max);
        ApplicableRegionSet overlaps = regions.getApplicableRegions(newRegion);
        
        for (ProtectedRegion pr : overlaps) {
            if (!pr.getId().equalsIgnoreCase(regionName)) {
                p.sendMessage(config.getMessage("region_overlap"));
                return;
            }
        }
        
        ProtectedCuboidRegion finalRegion = new ProtectedCuboidRegion(regionName, min, max);
        finalRegion.copyFrom(oldRegion);
        
        regions.addRegion(finalRegion);
        
        p.sendMessage(config.getMessage("region_moved", "", regionName, p.getName()));
    }

    private void handleAddOwner(CommandSender sender, String[] args) {
        handleModifyUser(sender, args, true, true);
    }
    
    private void handleRemoveOwner(CommandSender sender, String[] args) {
        handleModifyUser(sender, args, true, false);
    }

    private void handleAddMember(CommandSender sender, String[] args) {
        handleModifyUser(sender, args, false, true);
    }
    
    private void handleRemoveMember(CommandSender sender, String[] args) {
        handleModifyUser(sender, args, false, false);
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("hiielaprotect.admin")) {
            sender.sendMessage(config.getMessage("no_permission"));
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage(config.getMessage("plugin_reloaded"));
    }
    
    private void handleModifyUser(CommandSender sender, String[] args, boolean isOwner, boolean isAdd) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("player_only"));
            return;
        }
        Player p = (Player) sender;
        if (args.length < 2) {
            String usageKey = isOwner ? (isAdd ? "usage_addowner" : "usage_removeowner") : (isAdd ? "usage_addmember" : "usage_removemember");
            p.sendMessage(config.getMessage(usageKey));
            return;
        }
        
        String targetName = args[1];
        String regionName = null;
        
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(p.getWorld()));
        
        if (args.length >= 3) {
            String rArg = args[2];
            if (rArg.startsWith("#")) {
                regionName = rArg.substring(1);
            } else {
                try {
                    int num = Integer.parseInt(rArg);
                    regionName = p.getName().toLowerCase() + "_" + num;
                } catch (NumberFormatException e) {
                    String usageKey = isOwner ? (isAdd ? "usage_addowner" : "usage_removeowner") : (isAdd ? "usage_addmember" : "usage_removemember");
                    p.sendMessage(config.getMessage(usageKey));
                    return;
                }
            }
        } else {
            if (regions != null) {
                BlockVector3 loc = BukkitAdapter.asBlockVector(p.getLocation());
                ApplicableRegionSet set = regions.getApplicableRegions(loc);
                for (ProtectedRegion pr : set) {
                    if (!pr.getId().equalsIgnoreCase("__global__")) {
                        regionName = pr.getId();
                        break;
                    }
                }
            }
        }
        
        if (regionName == null || regions == null) {
            p.sendMessage(config.getMessage("region_not_found"));
            return;
        }
        
        ProtectedRegion region = regions.getRegion(regionName);
        if (region == null) {
            p.sendMessage(config.getMessage("region_not_found"));
            return;
        }
        
        if (!p.hasPermission("hiielaprotect.admin") && !region.getOwners().contains(p.getUniqueId())) {
            p.sendMessage(config.getMessage("not_owner"));
            return;
        }
        
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetId = target.getUniqueId();
        
        if (isOwner) {
            if (isAdd) {
                if (region.getOwners().contains(targetId)){
                    p.sendMessage(config.getMessage("target_already_added", targetName, regionName, p.getName()));
                    return;
                }
                region.getOwners().addPlayer(targetId);
                p.sendMessage(config.getMessage("owner_added", targetName, regionName, p.getName()));
            } else {
                if(Objects.equals(targetName, p.getName())){
                    p.sendMessage(config.getMessage("cannot_remove_yourself"));
                    return;
                }
                if (!region.getOwners().contains(targetId)){
                    p.sendMessage(config.getMessage("target_not_added", targetName, regionName, p.getName()));
                    return;
                }
                region.getOwners().removePlayer(targetId);
                p.sendMessage(config.getMessage("owner_removed", targetName, regionName, p.getName()));
            }
        } else {
            if (isAdd) {
                if (region.getMembers().contains(targetId)){
                    p.sendMessage(config.getMessage("target_already_added", targetName, regionName, p.getName()));
                    return;
                }
                region.getMembers().addPlayer(targetId);
                p.sendMessage(config.getMessage("member_added", targetName, regionName, p.getName()));
            } else {
                if (!region.getMembers().contains(targetId)){
                    p.sendMessage(config.getMessage("target_not_added", targetName, regionName, p.getName()));
                    return;
                }
                region.getMembers().removePlayer(targetId);
                p.sendMessage(config.getMessage("member_removed", targetName, regionName, p.getName()));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList(
                config.getSubcommandName("create"),
                config.getSubcommandName("remove"),
                config.getSubcommandName("move"),
                config.getSubcommandName("addowner"),
                config.getSubcommandName("removeowner"),
                config.getSubcommandName("addmember"),
                config.getSubcommandName("removemember"),
                config.getSubcommandName("reload")
            );
        }
        if (args.length == 2 && args[0].equalsIgnoreCase(config.getSubcommandName("remove"))) {
            return Arrays.asList("@here", "#region_name", "player_name");
        }
        return new ArrayList<>();
    }
}
