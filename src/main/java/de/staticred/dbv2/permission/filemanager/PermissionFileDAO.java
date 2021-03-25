package de.staticred.dbv2.permission.filemanager;

import de.staticred.dbv2.files.ConfigObject;
import de.staticred.dbv2.files.FileConstants;
import de.staticred.dbv2.files.filehandlers.FileManager;
import de.staticred.dbv2.permission.PermissionDAO;
import de.staticred.dbv2.util.BotHelper;
import net.dv8tion.jda.api.entities.Role;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.exceptions.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionFileDAO implements FileManager, PermissionDAO {

    public static final String NAME = "permissions.yml";


    /**
     * location of the file
     */
    private final File file;

    /**
     * conf object to work with
     */
    private YamlFile conf;

    /**
     * whether the file is valid or not
     */
    private boolean isValidFile;

    /**
     * Constructor.
     * @param file location
     */
    public PermissionFileDAO(File file) {
        this.file = file;
    }

    @Override
    public boolean isFilePresent() {
        if (!file.exists()) {
            //file does not exist

            file.getParentFile().mkdirs();

            //directories exist
            //now create the file
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(FileConstants.RESOURCE_LOCATION + getName())) {
                if (in == null) {
                    isValidFile = false;
                    throw new IOException("Can't read " + getName() + " from resource package");
                }

                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        }
        return isValidFile = true;
    }


    @Override
    public boolean isValidFile() {
        return isValidFile;
    }

    @Override
    public boolean isUpdatable(File updateAble) throws IllegalStateException, IllegalArgumentException {
        return false;
    }

    @Override
    public boolean updateFile(File newFile) {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ConfigObject getConfigObject() {
        return new ConfigObject(conf);
    }

    @Override
    public void set(String key, Object value) {
        conf.set(key, value);
        saveData();
    }

    @Override
    public boolean startDAO() throws IOException {
        if (!isFilePresent()) {
            isValidFile = false;
            throw new IOException("Can't create file at required location");
        }

        conf = new YamlFile(file);
        try {
            conf.load();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            isValidFile = false;
            return false;
        }

        //file does exist
        return isValidFile = true;
    }

    @Override
    public boolean saveData() {
        try {
            conf.save();
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        return true;
    }


    @Override
    public boolean hasPermission(long id, String permission) {
        Map<String, Boolean> permissionMap = getPermissions(id, false);

        if (!permissionMap.containsKey(permission))
            return false;

        return permissionMap.get(permission);
    }

    @Override
    public void setPermission(long id, String permission, boolean enabled) {
        String role = Long.toString(id);

        if (!conf.contains(role)) {
            addRole(id);
        }

        List<String> permissions = conf.getStringList(role + ".permission");

        permissions.add(permission);

        conf.set(role + ".permission", permissions);

        if (enabled) {
            List<String> enableds = conf.getStringList(role + ".enabled");
            enableds.add(permission);
            conf.set(role + ".enabled", permissions);
        }

        saveData();
    }

    public void addRole(long role) {
        conf.set(role + ".permission", new ArrayList<String>());
        conf.set(role + ".inherit", new ArrayList<String>());
        conf.set(role + ".enabled", new ArrayList<String>());
        saveData();
    }

    @Override
    public List<Role> getInheritingRoles(long role) {
        List<Role> returnList = new ArrayList<>();

        List<String> inheriting = conf.getStringList(role + ".inherit");

        for (String inherit : inheriting) {
            Role dcRole = BotHelper.jda.getRoleById(inherit);


            if (dcRole == null) {
                removeRole(Long.parseLong(inherit));
                continue;
            }
            returnList.add(dcRole);
        }

        return returnList;
    }

    public void removeRole(long role) {
        conf.set(Long.toString(role), null);
        saveData();
    }

    @Override
    public Map<String, Boolean> getPermissions(long role, boolean deep) {
        HashMap<String, Boolean> permissionMap = new HashMap<>();

        List<String> permissions = conf.getStringList(role + ".permission");
        List<String> enabled = conf.getStringList(role + ".enabled");

        for (String permission : permissions) {
            permissionMap.put(permission, enabled.contains(permission));
            if (deep) {
                List<String> inheriting = conf.getStringList(role + ".inherit");
                for (String inherit : inheriting) {
                    Map<String, Boolean> permissionInherit = getPermissions(Long.parseLong(inherit), true);
                    permissionMap.putAll(permissionInherit);
                }
            }
        }

        return permissionMap;
    }

    @Override
    public void removePermission(long role, String permission) {
        List<String> permissions = conf.getStringList(role + ".permission");
        List<String> enabled = conf.getStringList(role + ".enabled");


        permissions.remove(permission);
        enabled.remove(permission);

        conf.set(role + ".permission", permissions);
        conf.set(role + ".enabled", enabled);
        saveData();
    }

    @Override
    public void addInherit(long role, long inherit) {
        List<String> inheriting = conf.getStringList(role + ".inherit");
        inheriting.add(Long.toString(inherit));
        conf.set(role + ".inherit", inheriting);
        saveData();
    }

    @Override
    public void removeInherit(long role, long inherit) {
        List<String> inheriting = conf.getStringList(role + ".inherit");
        inheriting.remove(Long.toString(inherit));
        conf.set(role + ".inherit", inheriting);
        saveData();
    }


    @Override
    public void setEnabledState(long id, String permission, boolean state) {
        List<String> enabled = conf.getStringList(id + ".enabled");
        List<String> permissions = conf.getStringList(id + ".permission");

        if (!permissions.contains(permission))
            permissions.add(permission);

        if (state) {
            if (!enabled.contains(permission))
                enabled.add(permission);
        } else {
            enabled.remove(permission);
        }

        conf.set(id + ".permission", permissions);
        conf.set(id + ".enabled", enabled);
        saveData();
    }

    @Override
    public YamlFile asYaml() {
        return conf;
    }
}
