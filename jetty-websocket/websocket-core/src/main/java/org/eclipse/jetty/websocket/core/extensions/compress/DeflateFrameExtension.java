//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.core.extensions.compress;

import java.util.zip.DataFormatException;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.BadPayloadException;
import org.eclipse.jetty.websocket.core.frames.Frame;
import org.eclipse.jetty.websocket.core.frames.Frame;

/**
 * Implementation of the
 * <a href="https://tools.ietf.org/id/draft-tyoshino-hybi-websocket-perframe-deflate.txt">deflate-frame</a>
 * extension seen out in the wild.
 */
public class DeflateFrameExtension extends CompressExtension
{
    @Override
    public String getName()
    {
        return "deflate-frame";
    }
    
    @Override
    int getRsvUseMode()
    {
        return RSV_USE_ALWAYS;
    }
    
    @Override
    int getTailDropMode()
    {
        return TAIL_DROP_ALWAYS;
    }

    @Override
    public void onReceiveFrame(Frame frame, Callback callback)
    {
        // Incoming frames are always non concurrent because
        // they are read and parsed with a single thread, and
        // therefore there is no need for synchronization.

        if ( frame.getType().isControl() || !frame.isRsv1() || !frame.hasPayload() )
        {
            nextIncomingFrame(frame, callback);
            return;
        }

        try
        {
            ByteAccumulator accumulator = new ByteAccumulator(getPolicy().getMaxAllowedFrameSize());
            decompress(accumulator, frame.getPayload());
            decompress(accumulator, TAIL_BYTES_BUF.slice());
            forwardIncoming(frame, callback, accumulator);
        }
        catch (DataFormatException e)
        {
            throw new BadPayloadException(e);
        }
    }
}