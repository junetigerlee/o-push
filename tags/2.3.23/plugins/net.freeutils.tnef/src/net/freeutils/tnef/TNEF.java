/*
 *  (c) copyright 2003-2009 Amichai Rothman
 *
 *  This file is part of the Java TNEF package.
 *
 *  The Java TNEF package is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  The Java TNEF package is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.freeutils.tnef;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The <code>TNEF</code> class provides high-level utility methods to
 * access TNEF streams and extract their contents.
 *
 * Note: This class is experimental and is intended to show possible uses
 * of the Java TNEF package.
 *
 * @author Amichai Rothman
 * @since 2003-04-25
 */
public class TNEF {

    /**
     * Extracts the content from the given TNEFInputStream, saving attachments
     * to the given output directory.
     *
     * @param in the TNEFInputStream containing the content
     * @param outputdir the directory in which attachments should be saved
     * @return the number of attachments that were saved
     * @throws IOException if an I/O error occurs
     */
    public static int extractContent(TNEFInputStream in, String outputdir) throws IOException {
        return extractContent(new Message(in), outputdir);
    }

    /**
     * Extracts the content from the given TNEF Message, saving attachments
     * to the given output directory.
     *
     * @param message the Message containing the content
     * @param outputdir the directory in which attachments should be saved
     * @return the number of attachments that were saved
     * @throws IOException if an I/O error occurs
     */
    public static int extractContent(Message message, String outputdir) throws IOException {
        if (!outputdir.endsWith(File.separator))
            outputdir += File.separator;
        // handle attributes
        System.out.println("\nMessage Attributes:\n");
        List<Attr> attributes = message.getAttributes();
        for (int i = 0; i < attributes.size(); i++)
            System.out.println(attributes.get(i).toString());

        // handle attachments
        System.out.println("\nMessage Attachments:\n");
        List<Attachment> attachments = message.getAttachments();
        int count = 0;
        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get(i);
            System.out.println(attachment.toString());
            if (attachment.getNestedMessage() != null) { // nested message
                count += extractContent(attachment.getNestedMessage(), outputdir);
            } else { // regular attachment
                count++;
                String attachFilename = attachment.getFilename();
                String filename = outputdir +
                    (attachFilename == null ? ("attachment" + count) : attachFilename);
                System.out.println("\n>>> Writing attachment #" + count + " to " + filename + "\n");
                attachment.writeTo(filename);
            }
        }
        System.out.println("\nWrote " + count + " attachments.");
        return count;
    }

    /**
     * Main entry point for command-line utility.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        String tneffile = null;
        String outputdir = null;

        if (args.length == 2) {
            tneffile = args[0];
            outputdir = args[1];
        } else {
            System.out.println("Usage: java net.freeutils.tnef.TNEF <tneffile> <outputdir>");
            System.out.println("\nexample: java net.freeutils.tnef.TNEF c:\\temp\\winmail.dat c:\\temp\\attachments");
            System.exit(1);
        }

        System.out.println("Processing TNEF file " + tneffile);
        System.out.println();

        RawInputStream fis = null;
        TNEFInputStream in = null;
        try {
            fis = new RawInputStream(tneffile);
            in = new TNEFInputStream(tneffile);
            extractContent(in, outputdir);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Operation aborted.");
            System.exit(-1);
        } finally {
            try {
                if (in != null)
                    in.close();
                if (fis != null)
                    fis.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        System.out.println();
        System.out.println("Finished processing TNEF file " + tneffile);
    }

}
