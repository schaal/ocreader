package email.schaal.ocreader.api.json;

import java.util.List;

import email.schaal.ocreader.database.model.Folder;

/**
 * Class to deserialize the json response for folders
 */
public class Folders {
    public List<Folder> getFolders() {
        return folders;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }

    private List<Folder> folders;
}
