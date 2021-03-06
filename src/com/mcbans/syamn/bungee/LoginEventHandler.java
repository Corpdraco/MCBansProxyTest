/**
 * MCBansProxy - Package: com.mcbans.syamn.bungee
 * Created: 2013/01/26 23:24:09
 */
package com.mcbans.syamn.bungee;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * LoginEventHandler (LoginEventHandler.java)
 */
public class LoginEventHandler implements Listener{
    private static final String logPrefix = MCBansProxy.logPrefix;
    private MCBansProxy plugin;
    public final String apiRequestSuffix = "4.3.4";
    
    LoginEventHandler(final MCBansProxy plugin){
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onLogin(final LoginEvent event){
        final PendingConnection pc = event.getConnection();
        if (event.isCancelled() || pc == null) return;
        
        if (!plugin.isValidKey){
            ProxyServer.getInstance().getLogger().warning("Missing or invalid API Key! Please check config.yml and restart proxy!");
            return;
        }
        
        try{
            final String uriStr = "http://api.mcbans.com/v3/" + plugin.apiKey + "/login/"
                    + URLEncoder.encode(pc.getUUID().toString().replaceAll("-", ""), "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(pc.getAddress().getAddress()), "UTF-8") + "/"
                    + apiRequestSuffix;
            final URLConnection conn = new URL(uriStr).openConnection();
            
            conn.setConnectTimeout(plugin.timeout * 1000);
            conn.setReadTimeout(plugin.timeout * 1000);
            conn.setUseCaches(false);
            
            BufferedReader br = null;
            String response = null;
            try{
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                response = br.readLine();
            }finally{
                if (br != null) br.close();
            }
            if (response == null){
                if (plugin.failsafe){
                    ProxyServer.getInstance().getLogger().info("Null response! Kicked player: " + pc.getName());
                    event.setCancelled(true);
                    event.setCancelReason("MCBans service unavailable!");
                }else{
                    ProxyServer.getInstance().getLogger().info(logPrefix + "Null response! Check passed player: " + pc.getName());
                }
                return;
            }
            
            plugin.debug("Response: " + response);
            String[] s = response.split(";");
            if (s.length >= 5) {
	            if(s.length>=2){
	            	if (s[0].equals("l") || s[0].equals("g") || s[0].equals("t") || s[0].equals("i") || s[0].equals("s")) {
	                    event.setCancelled(true);
	                    String[] reasonData = s[1].split("\\$");
	                    
	                    event.setCancelReason(ChatColor.YELLOW+"Reason: "+ChatColor.RED+reasonData[0]+"\n"+ChatColor.YELLOW+"Ban from: "+ChatColor.WHITE+reasonData[1]+"\n"+ChatColor.YELLOW+"Ban Type: "+((reasonData[3].equalsIgnoreCase("global"))?ChatColor.GOLD:ChatColor.GRAY)+reasonData[3]+"\n"+ChatColor.AQUA+"http://mcbans.com/ban/"+reasonData[2]);
	                    return;
	                }
	            }
	            if (plugin.minRep > Double.valueOf(s[2])) {
                    event.setCancelled(true);
                    event.setCancelReason(plugin.minRepMsg);
                    return;
                }
	            if(Integer.parseInt(s[3]) > 0){
	            	if (plugin.enableMaxAlts && plugin.maxAlts < Integer.valueOf(s[3])) {
	                    event.setCancelled(true);
	                    event.setCancelReason(plugin.maxAltsMsg);
	                    return;
	                }
	            }
	            if(s[0].equals("b")){
                    ProxyServer.getInstance().getLogger().info(logPrefix + pc.getName() + " has previous ban(s)!");
                }
                if(Integer.parseInt(s[3])>0){
                    ProxyServer.getInstance().getLogger().info(logPrefix + pc.getName() + " may has " + s[3] + " alt account(s)![" + s[6] + "]");
                }
                if(s[4].equals("y")){
                    ProxyServer.getInstance().getLogger().info(logPrefix + pc.getName() + " is an MCBans.com Staff Member!");
                }
                if(Integer.parseInt(s[5])>0){
                    ProxyServer.getInstance().getLogger().info(logPrefix + s[5] + " open dispute(s)!");
                }
	            plugin.debug(pc.getName() + " authenticated with " + s[2] + " rep");
            }else{
                if (response.toString().contains("Server Disabled")) {
                    ProxyServer.getInstance().getLogger().info(logPrefix + "This Server Disabled by MCBans Administration!");
                    return;
                }
                if (plugin.failsafe){
                    ProxyServer.getInstance().getLogger().info(logPrefix + "Null response! Kicked player: " + pc.getName());
                    event.setCancelled(true);
                    event.setCancelReason(plugin.unavailable);
                }else{
                    ProxyServer.getInstance().getLogger().info(logPrefix + "Invalid response!(" + s.length + ") Check passed player: " + pc.getName());
                }
                ProxyServer.getInstance().getLogger().info(logPrefix + "Response: " + response);
                return;
            }
        }catch (SocketTimeoutException ex){
            ProxyServer.getInstance().getLogger().info(logPrefix + "Cannot connect MCBans API server: timeout");
            if (plugin.failsafe){
                event.setCancelled(true);
                event.setCancelReason(plugin.unavailable);
            }
        }catch (Exception ex){
            ProxyServer.getInstance().getLogger().info(logPrefix + "Cannot connect MCBans API server!");
            if (plugin.failsafe){
                event.setCancelled(true);
                event.setCancelReason(plugin.unavailable);
            }
            if (plugin.isDebug) ex.printStackTrace();
        }
    }
}
