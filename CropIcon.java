import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class CropIcon {
    public static void main(String[] args) {
        try {
            File inputFile = new File("C:/Users/alexa/.gemini/antigravity/brain/04c2cee3-e97f-4317-ac99-86ad1f117a02/.user_uploaded/media_1788119539034.jpg");
            BufferedImage img = ImageIO.read(inputFile);
            System.out.println("Image read: " + img.getWidth() + "x" + img.getHeight());
            
            // From the styleguide, the logo is at top left.
            // We just need to estimate the coordinates. Or we can just use the previous relatopro_logo_1788092210630.jpg 
            File existingLogo = new File("C:/Users/alexa/.gemini/antigravity/brain/04c2cee3-e97f-4317-ac99-86ad1f117a02/relatopro_logo_1788092210630.jpg");
            BufferedImage logo = ImageIO.read(existingLogo);
            System.out.println("Logo read: " + logo.getWidth() + "x" + logo.getHeight());
            
            // Save as PNG
            File output = new File("app/src/main/res/drawable/ic_launcher_foreground.png");
            ImageIO.write(logo, "png", output);
            System.out.println("Saved PNG icon.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
