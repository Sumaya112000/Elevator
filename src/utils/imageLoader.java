package utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Stream;
import javafx.scene.image.Image;

/**
 * Utility class to load in the PFD images for the GUI.
 */
public class imageLoader {
    // All images in res
    public final ArrayList<Image> imageList = new ArrayList<>();
    // Path to res folder
    private final String IMAGE_PATH = "res/";

    /**
     * Function to load the image resources to the imageList array.
     */
    public void loadImages(){
        try (Stream<Path> s = Files.walk(Paths.get(IMAGE_PATH))) {

            // File filtering
            Stream<Path> images = s.filter((Path p) -> {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".png");
            });

            // Add all image files to the array list
            images.forEach(i -> {
                String uri = i.toUri().toString();
                Image img = new Image(uri, true);
                imageList.add(img);
            });

            // Sort images in array so indexing is the same across systems
            imageList.sort((img1, img2) -> {
                String n1 = img1.getUrl();
                String n2 = img2.getUrl();
                return n1.compareToIgnoreCase(n2);
            });

        } catch (Exception e) { e.printStackTrace(); }
    }
}