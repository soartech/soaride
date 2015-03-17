/**
 * 
 */
package com.soartech.soar.ide.ui.views.console;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * @author aron
 *
 */
public class TestMessageConsole extends MessageConsole {
    private MessageConsoleStream outMessageStream;
    private MessageConsoleStream inMessageStream;

//    private PacketListener outListener = new PacketListener() {
//      public void processPacket(Packet arg0) {
//        outMessageStream.println(arg0.toXML());
//      }
//    };
//
//    private PacketListener inListener = new PacketListener() {
//      public void processPacket(Packet arg0) {
//        inMessageStream.println(arg0.toXML());
//      }
//    };

    public TestMessageConsole() {
      super("Test Console", null);
      outMessageStream = newMessageStream();
      outMessageStream.setColor(Display.getCurrent().getSystemColor(
          SWT.COLOR_BLUE));
      inMessageStream = newMessageStream();
      inMessageStream.setColor(Display.getCurrent().getSystemColor(
          SWT.COLOR_RED));

//      Session.getInstance().getConnection().
//          addPacketWriterListener(outListener, null);
//      Session.getInstance().getConnection().
//          addPacketListener(inListener, null);
    }

    protected void dispose() {
//      Session.getInstance().getConnection().removePacketWriterListener(
//          outListener);
//      Session.getInstance().getConnection().removePacketListener(inListener);
    }
}
