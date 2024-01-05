import gearth.extensions.IExtension;
import gearth.extensions.extra.tools.ChatInputListener;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Bot {
    private final int chatid;

    private final String name;

    private final String figureString;

    private final String infoMessage;

    private volatile boolean firstTime;

    private final Observable<ChatInputListener> chatInputObservable;

    private final IExtension extension;

    public Bot(IExtension extension, String infoMessage, String name, String figure_string) {
        this.name = name;
        if (figure_string == null)
            figure_string = "hr-5124-61.hd-209-14.ch-262-110.lg-280-1427.sh-305-92.he-1608.ea-5392.cp-3124-110";
        this.figureString = figure_string;
        this.firstTime = true;
        this.chatInputObservable = new Observable<>();
        this.extension = extension;
        this.chatid = this.name.hashCode() % 300000000 + 300000000;
        this.infoMessage = infoMessage;
        AtomicBoolean doOncePerConnection = new AtomicBoolean(false);

        extension.onConnect((host, port, hotelversion, clientIdentifier, clientType) -> doOncePerConnection.set(true));
        extension.intercept(HMessage.Direction.TOSERVER, hMessage -> {
            if (this.firstTime) {
                this.firstTime = false;
                if (hMessage.getPacket().headerId() != 4000) {
                    doOncePerConnection.set(false);
                    createChat();
                }
            }
        });
        extension.intercept(HMessage.Direction.TOCLIENT, "MessengerError", hMessage -> {
            if (UserAdder.isOn)
                hMessage.setBlocked(true);
        });
        extension.intercept(HMessage.Direction.TOCLIENT, "FriendListFragment", hMessage -> {
            if (doOncePerConnection.get()) {
                doOncePerConnection.set(false);
                (new Thread(() -> {

                })).start();
            }
        });
        extension.intercept(HMessage.Direction.TOSERVER, "SendMsg", hMessage -> {
            HPacket packet = hMessage.getPacket();
            if (packet.readInteger() == this.chatid) {
                hMessage.setBlocked(true);
                String s = packet.readString();
                if (s.equals("/info") && infoMessage != null) {
                    writeOutput("Extension initially made by schweppes-0x, updated by Thauan !", false);
                } else {
                    try {
                        if (!s.startsWith("/"))
                            return;
                        s = s.replace("/", "");
                        switch (s.toLowerCase()) {
                            case "on":
                                UserAdder.isOn = true;
                                this.writeOutput("[!] - You enabled me.", false);
                                return;
                            case "off":
                                UserAdder.isOn = false;
                                this.writeOutput("[!] - You disabled me.", false);
                                return;
                            case "notify":
                                UserAdder.showInfo = !UserAdder.showInfo;
                                this.writeOutput("[!] - Notify when Done: " + UserAdder.showInfo, false);
                                return;
                        }
                        this.writeOutput("[!] - Wrong command. try /on or /off", false);
                    } catch (Exception e) {
                        this.writeOutput("[x] - Error", false);
                    }
                }
            }
        });
    }

    private void createChat() {
        HPacket packet = new HPacket("FriendListUpdate", HMessage.Direction.TOCLIENT, 0, 1, 0, this.chatid, this.name, 1, true, false, this.figureString, 0, "", 0, true, false, true, "");

        this.extension.sendToClient(packet);
        if (this.infoMessage != null)
            writeOutput(this.infoMessage, false);
    }

    public void writeOutput(String string, boolean asInvite) {
        byte[] array = new byte[7];
        new Random().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);

        this.extension.sendToClient(new HPacket("NewConsole", HMessage.Direction.TOCLIENT, this.chatid, string, 0, generatedString, 0, this.chatid, "Thauan", figureString));
    }

}
