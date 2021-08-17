package rikka.shizuku.service;

import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moe.shizuku.server.IShizukuApplication;
import rikka.shizuku.service.util.Logger;

public class ClientManager<ConfigMgr extends ConfigManager> {

    private static final Logger LOGGER = new Logger("UserServiceRecord");

    private final ConfigMgr configManager;
    private final List<ClientRecord> clientRecords = Collections.synchronizedList(new ArrayList<>());

    public ClientManager(ConfigMgr configManager) {
        this.configManager = configManager;
    }

    public ConfigMgr getConfigManager() {
        return configManager;
    }

    public List<ClientRecord> findClients(int uid) {
        synchronized (this) {
            List<ClientRecord> res = new ArrayList<>();
            for (ClientRecord clientRecord : clientRecords) {
                if (clientRecord.uid == uid) {
                    res.add(clientRecord);
                }
            }
            return res;
        }
    }

    public ClientRecord findClient(int uid, int pid) {
        for (ClientRecord clientRecord : clientRecords) {
            if (clientRecord.pid == pid && clientRecord.uid == uid) {
                return clientRecord;
            }
        }
        return null;
    }

    public ClientRecord addClient(int uid, int pid, IShizukuApplication client, String packageName) {
        ClientRecord clientRecord = new ClientRecord(uid, pid, client, packageName);

        ConfigPackageEntry entry = configManager.find(uid);
        if (entry != null && entry.isAllowed()) {
            clientRecord.allowed = true;
        }

        IBinder binder = client.asBinder();
        IBinder.DeathRecipient deathRecipient = (IBinder.DeathRecipient) () -> clientRecords.remove(clientRecord);
        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            LOGGER.w(e, "addClient: linkToDeath failed");
            return null;
        }

        clientRecords.add(clientRecord);
        return clientRecord;
    }
}


