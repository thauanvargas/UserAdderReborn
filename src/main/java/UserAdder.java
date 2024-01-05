import gearth.extensions.Extension;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;


@ExtensionInfo(
        Title = "UserAdder Reborn",
        Description = "It will add all users upon entering a room.",
        Version = "1.0",
        Author = "schweppes-0x, updated by Thauan"
)

public class UserAdder extends Extension {

    public UserAdder(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        new UserAdder(args).run();
    }

    public static UserAdder RUNNING_INSTANCE;

    public static boolean isOn;

    public static boolean isSent;

    public static boolean showInfo = true;

    private final Set<HEntity> loadedUsers = new HashSet<>();

    public int habboId;

    @Override
    protected void onStartConnection() {
    }

    @Override
    protected void initExtension() {
        RUNNING_INSTANCE = this;

        String infoMessage = "Welcome, commands: /on , /off , /notify , /info";
        Bot bot = new Bot(this, infoMessage, "Thauan", null);

        intercept(HMessage.Direction.TOCLIENT, "RoomReady", hMessage -> {
            if (!isOn)
                return;
            this.loadedUsers.clear();
            isSent = false;
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            if (!isOn)
                return;
            HPacket hPacket = hMessage.getPacket();
            HEntity[] roomUsersList = HEntity.parse(hPacket);
            for (HEntity hEntity : roomUsersList) {
                if (hEntity.getId() != habboId) {
                    loadedUsers.add(hEntity);
                }
            }
            if (this.loadedUsers.size() > 0)
                new Thread(this::addUsers).start();
        });

    }

    private void addUsers() {
        if (isOn && !isSent) {
            int total = 0;

            Set<HEntity> users = new HashSet<>(loadedUsers);

            for (HEntity entity : users) {
                if (sendToServer(new HPacket("{out:RequestFriend}{s:\"" + entity.getName() + "\"}")))
                    total++;
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException ignored) {}
            }
            isSent = true;
            if (showInfo)
                sendToServer(new HPacket("{out:Whisper}{s:\"Thauan I have added " + total + " users in this room. \"}{i:0}"));
        }
    }


}
