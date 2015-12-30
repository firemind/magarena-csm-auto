package magic.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import magic.data.CardDefinitions;
import magic.data.DuelConfig;
import magic.data.GeneralConfig;
import magic.model.MagicLogger;
import magic.model.player.PlayerProfiles;
import magic.translate.UiString;
import magic.utility.FileIO;
import magic.utility.MagicFileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;

public class ImportWorker extends SwingWorker<Boolean, Void> {

    // translatable strings
    private static final String _S1 = "FAIL";
    private static final String _S2 = "- new cards snapshot...";
    private static final String _S3 = "OK";
    private static final String _S4 = "- themes...";
    private static final String _S5 = "- preferences...";
    private static final String _S6 = "- avatars...";
    private static final String _S7 = "- custom decks...";
    private static final String _S8 = "- player profiles...";
    private static final String _S9 = "- new duel settings...";
    private static final String _S10 = "- card images...";
    private static final String _S11 = "There was problem during the import process.";
    private static final String _S12 = "Please see the following file for more details -";

    private static final String OK_STRING = String.format("%s\n", UiString.get(_S3));
    private static final String FAIL_STRING = String.format("%s\n", UiString.get(_S1));

    private final Path importDataPath;
    private String progressNote = "";
    private final MagicLogger logger;
    private boolean isFailed = false;

    public ImportWorker(final File magarenaDirectory) {
        this.importDataPath = magarenaDirectory.toPath().resolve(MagicFileSystem.DATA_DIRECTORY_NAME);
        logger = new MagicLogger("import", "Magarena Import Log");
    }

    @Override
    public Boolean doInBackground() throws IOException {
        if (!isCancelled()) { importPreferences(); }
        if (!isCancelled()) { importNewDuelConfig(); }
        if (!isCancelled()) { importPlayerProfiles(); }
        if (!isCancelled()) { importCustomDecks(); }
        if (!isCancelled()) { importAvatars(); }
        if (!isCancelled()) { importMods(); }
        if (!isCancelled()) { importCardImages(); }
        if (!isCancelled()) { updateNewCardsLog(); }
        CachedImagesProvider.getInstance().clearCache();
        return !isFailed;
    }

    @Override
    public void done() {

        boolean isOk = true;

        try {
            isOk = get();
        } catch (InterruptedException | ExecutionException ex) {
            System.err.println(ex.getCause());
            logger.log(ex.getCause().toString());
            setProgressNote(FAIL_STRING);
            isOk = false;
        } catch (CancellationException ex) {
            // cancelled by user.
        }

        setProgressNote("");
        setProgress(0);
        logger.writeLog();

        if (!isOk) {
            ScreenController.showWarningMessage(String.format("%s\n\n%s\n%s",
                UiString.get(_S11),
                UiString.get(_S12),
                "...\\Magarena\\logs\\import.log")
            );
        }

    }

    /**
     * Rebuilds the "newcards.log" file so that it contains all the new playable cards which
     * have been added since the imported and current versions.
     */
    private void updateNewCardsLog() {
        setProgressNote(UiString.get(_S2));
        setProgress(0);
        final File scriptsDirectory = this.importDataPath.resolve("scripts").toFile();
        final File[] scriptFiles = MagicFileSystem.getSortedScriptFiles(scriptsDirectory);
        final List<String> cards = new ArrayList<>();
        final int countMax = scriptFiles.length;
        int count = 0;
        for (final File file : scriptFiles) {
            final Properties content = FileIO.toProp(file);
            cards.add(content.getProperty("name"));
            count++;
            setProgress((int)((count / (double)countMax) * 100));

        }
        CardDefinitions.updateNewCardsLog(cards);
        setProgressNote(OK_STRING);
    }

    /**
     * Merges "mods" folder and sub-folders.
     * If file already exists then imported version takes precedence.
     */
    private void importMods() throws IOException {
        setProgressNote(UiString.get(_S4));
        final String directoryName = "mods";
        final Path sourcePath = importDataPath.resolve(directoryName);
        if (sourcePath.toFile().exists()) {
            final Path targetPath = MagicFileSystem.getDataPath().resolve(directoryName);
            FileUtils.copyDirectory(sourcePath.toFile(), targetPath.toFile(), getModsFileFilter());
        }
        setProgressNote(OK_STRING);
    }

    /**
     * Creates a filter that returns everything in the "mods" folder except
     * predefined cubes which are distributed with each new release.
     */
    private FileFilter getModsFileFilter() {
        final String[] excludedCubes = new String[]{
            "legacy_cube.txt", "modern_cube.txt", "standard_cube.txt", "extended_cube.txt", "ubeefx_cube.txt"
        };
        final IOFileFilter excludedFiles = new NameFileFilter(excludedCubes, IOCase.INSENSITIVE);
        final IOFileFilter excludeFilter = FileFilterUtils.notFileFilter(excludedFiles);
        return FileFilterUtils.or(DirectoryFileFilter.DIRECTORY, excludeFilter);
    }

    /**
     * Merges "general.cfg" file.
     * If setting already exists then imported value takes precedence.
     */
    private void importPreferences() throws IOException {
        setProgressNote(UiString.get(_S5));

        final String CONFIG_FILENAME = "general.cfg";

        // Create new config file with default settings.
        final File thisConfigFile = MagicFileSystem.getDataPath().resolve(CONFIG_FILENAME).toFile();
        if (thisConfigFile.exists()) {
            thisConfigFile.delete();
        }
        GeneralConfig.getInstance().save();

        final Properties thisProperties = FileIO.toProp(thisConfigFile);
        final Properties theirProperties = FileIO.toProp(importDataPath.resolve(CONFIG_FILENAME).toFile());

        // list of latest config settings.
        final List<String> thisSettings = new ArrayList<>(thisProperties.stringPropertyNames());

        // not interested in importing these settings.
        thisSettings.removeAll(Arrays.asList(
                "left", 
                "fullScreen", 
                "top", 
                "height", 
                "width",
                "translation"));

        // import settings...
        for (String setting : thisSettings) {
            if (theirProperties.containsKey(setting)) {
                thisProperties.setProperty(setting, theirProperties.getProperty(setting));
            }
        }

        // save updated preferences and reload.
        FileIO.toFile(thisConfigFile, thisProperties, "General configuration");
        GeneralConfig.getInstance().load();

        // override download dates to catch any missed "image_updated" updates.
        final Calendar dt = Calendar.getInstance();
        dt.add(Calendar.DAY_OF_MONTH, -60);
        GeneralConfig.getInstance().setMissingImagesDownloadDate(dt.getTime());
        GeneralConfig.getInstance().setPlayableImagesDownloadDate(dt.getTime());
        GeneralConfig.getInstance().save();
        
        setProgressNote(OK_STRING);
    }

    /**
     * Merges "avatars" folder and sub-folders.
     * If file already exists then imported version takes precedence.
     */
    private void importAvatars() throws IOException {
        setProgressNote(UiString.get(_S6));
        final String directoryName = "avatars";
        final Path sourcePath = importDataPath.resolve(directoryName);
        if (sourcePath.toFile().exists()) {
            final Path targetPath = MagicFileSystem.getDataPath().resolve(directoryName);
            FileUtils.copyDirectory(sourcePath.toFile(), targetPath.toFile());
        }
        setProgressNote(OK_STRING);
    }

    /**
     * Merges top level "decks" folder only.
     * Does not import sub-folders (prebuilt, firemind, etc).
     * If file already exists then imported version takes precedence.
     */
    private void importCustomDecks() throws IOException {
        setProgressNote(UiString.get(_S7));
        final String directoryName = "decks";
        final Path sourcePath = importDataPath.resolve(directoryName);
        if (sourcePath.toFile().exists()) {
            final Path targetPath = MagicFileSystem.getDataPath().resolve(directoryName);
            final IOFileFilter deckSuffixFilter = FileFilterUtils.suffixFileFilter(".dec");
            FileUtils.copyDirectory(sourcePath.toFile(), targetPath.toFile(), deckSuffixFilter);
        }
        setProgressNote(OK_STRING);
    }

    /**
     * Delete "players" folder and replace with imported copy.
     */
    private void importPlayerProfiles() throws IOException {
        setProgressNote(UiString.get(_S8));
        final String directoryName = "players";
        final Path targetPath = MagicFileSystem.getDataPath().resolve(directoryName);
        FileUtils.deleteDirectory(targetPath.toFile());
        final Path sourcePath = importDataPath.resolve(directoryName);
        if (sourcePath.toFile().exists()) {
            FileUtils.copyDirectory(sourcePath.toFile(), targetPath.toFile());
            PlayerProfiles.refreshMap();
        }
        setProgressNote(OK_STRING);
    }

    /**
     * Delete "duels" folder and replace with imported copy.
     */
    private void importNewDuelConfig() {
        setProgressNote(UiString.get(_S9));
        boolean isOk = true;
        final String directoryName = "duels";
        final Path targetPath = MagicFileSystem.getDataPath().resolve(directoryName);
        final Path sourcePath = importDataPath.resolve(directoryName);
        if (sourcePath.toFile().exists()) {
            try {
                FileUtils.deleteDirectory(targetPath.toFile());
                FileUtils.copyDirectory(sourcePath.toFile(), targetPath.toFile());
            } catch (IOException ex) {
                System.err.println(ex);
                logger.log(ex.toString());
                isOk = false;
            }
            DuelConfig.getInstance().load();
        }
        setProgressNote(isOk ? OK_STRING : FAIL_STRING);
    }

    /**
     * Moves an existing images folder to the current "\Magarena\images" folder.
     * <p>
     * Normally this will just be a case of "renaming" the folder which will be
     * instantaneous. However if the new location is on a different drive or
     * filesystem then a "copy and delete" operation will be required which
     * could take some time depending on how many image files are in the folder
     * to be moved.
     */
    private boolean moveImages(final String directoryName) {

        boolean isOk = true;

        final File imagesFolder = MagicFileSystem.getDataPath(MagicFileSystem.DataPath.IMAGES).toFile();

        // pre-version 1.67: default images folder location = "\Magarena".
        File importFolder = new File(importDataPath.toFile(), directoryName);
        if (!importFolder.exists()) {
            // version 1.67+: default images folder location = "\Magarena\<images>".
            final String folderName = MagicFileSystem.DataPath.IMAGES.getPath().getFileName().toString();
            importFolder = new File(importDataPath.resolve(folderName).toFile(), directoryName);
        }

        if (importFolder.exists()) {
            try {
                FileUtils.moveDirectoryToDirectory(importFolder, imagesFolder, true);
            } catch (IOException ex) {
                System.err.println(ex);
                logger.log(ex.toString());
                isOk = false;
            }
        }

        return isOk;
    }

    private void importCardImages() {

        setProgressNote(UiString.get(_S10));

        // skip if user-defined location. This is stored in the general.cfg file.
        if (GeneralConfig.getInstance().isCustomCardImagesPath()) {
            setProgressNote(OK_STRING);
            return;
        }

        String result = OK_STRING;
        if (!moveImages(MagicFileSystem.TOKEN_IMAGE_FOLDER)) {
            result = FAIL_STRING;
        }
        if (!moveImages(MagicFileSystem.CARD_IMAGE_FOLDER)) {
            result = FAIL_STRING;
        }
        setProgressNote(result);

        isFailed = !OK_STRING.equals(result);
    }

    private void setProgressNote(final String progressNote) {
        if (getPropertyChangeSupport().hasListeners("progressNote")) {
            firePropertyChange("progressNote", this.progressNote, progressNote);
        }
        this.progressNote = progressNote;
        if (!progressNote.isEmpty()) {
            logger.log(progressNote);
        }
    }
    
}
