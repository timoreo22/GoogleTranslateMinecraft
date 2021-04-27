package fr.timoreo.trans.mixin;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import org.apache.commons.lang3.StringEscapeUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMessageMixin {

    private static final Pattern p = Pattern.compile("class=\"result-container\">([^<]*)<\\/div>", Pattern.MULTILINE);
    private static final Pattern NO_USERNAME = Pattern.compile("<(.+?)> (.+)", Pattern.MULTILINE);
    @Shadow
    private MinecraftClient client;

    @Inject(method = {"onGameMessage"}, at =
            //when a method is invoked   place code after it
    @At(value = "INVOKE", shift = At.Shift.AFTER,
            //the method in itself,  forceMainThread(Packet<T> packet, T listener, ThreadExecutor<?> engine)
            target = "net/minecraft/network/NetworkThreadUtils.forceMainThread" +
                    "(Lnet/minecraft/network/Packet;" +
                    "Lnet/minecraft/network/listener/PacketListener;" +
                    "Lnet/minecraft/util/thread/ThreadExecutor;)V"))
    public void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        //packet.getMessage()
        LiteralText t = new LiteralText("");

        ((MutableText) packet.getMessage()).setStyle(packet.getMessage().getStyle().withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(t)));
        new Thread(() -> {
            String msg = packet.getMessage().getString();
            //remove username
            Matcher mu = NO_USERNAME.matcher(msg);
            mu.find();
            try {
                msg = mu.group(2);
            } catch (Exception e) {
                //oops bad chat format ???
                System.out.println("ERROR ! Bad chat format !");
            }

            String enc = "";
            try {
                enc = URLEncoder.encode(msg, "utf-8");
            } catch (UnsupportedEncodingException ignored) {
            }
            try {
                URL u = new URL("https://translate.google.com/m?sl=auto&tl=" + client.getLanguageManager().getLanguage().getCode().split("_")[0] + "&hl=en&ie=UTF-8&prev=_m&&q=" + enc);
                HttpURLConnection con = ((HttpURLConnection) u.openConnection());
                con.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                con.connect();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Matcher m = p.matcher(sb.toString());
                m.find();
                t.append(StringEscapeUtils.unescapeHtml4(m.group(1)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        //this.client.inGameHud.addChatMessage(packet.getLocation(), new LiteralText("Insert Translation Here"), packet.getSenderUuid());
    }

}
