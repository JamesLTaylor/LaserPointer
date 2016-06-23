/*
    Copyright (c) 2007-2014 Contributors as noted in the AUTHORS file

    This file is part of 0MQ.

    0MQ is free software; you can redistribute it and/or modify it under
    the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    0MQ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package zmq;

import java.nio.ByteBuffer;

//  Helper base class for decoders that know the amount of data to read
//  in advance at any moment. Knowing the amount in advance is a property
//  of the protocol used. 0MQ framing protocol is based size-prefixed
//  paradigm, which qualifies it to be parsed by this class.
//  On the other hand, XML-based transports (like XMPP or SOAP) don't allow
//  for knowing the size of data to read in advance and should use different
//  decoding algorithms.
//
//  This class implements the state machine that parses the incoming buffer.
//  Derived class should implement individual state machine actions.

public abstract class DecoderBase implements IDecoder
{
    //  Where to store the read data.
    private byte[] readBuf;
    private int readPos;

    //  How much data to read before taking next step.
    protected int toRead;

    //  The buffer for data to decode.
    private int bufsize;
    private ByteBuffer buf;

    private int state;

    boolean zeroCopy;

    public DecoderBase(int bufsize)
    {
        state = -1;
        toRead = 0;
        this.bufsize = bufsize;
        if (bufsize > 0) {
            buf = ByteBuffer.allocateDirect(bufsize);
        }
        readBuf = null;
        zeroCopy = false;
    }

    //  Returns a buffer to be filled with binary data.
    public ByteBuffer getBuffer()
    {
        //  If we are expected to read large message, we'll opt for zero-
        //  copy, i.e. we'll ask caller to fill the data directly to the
        //  message. Note that subsequent read(s) are non-blocking, thus
        //  each single read reads at most SO_RCVBUF bytes at once not
        //  depending on how large is the chunk returned from here.
        //  As a consequence, large messages being received won't block
        //  other engines running in the same I/O thread for excessive
        //  amounts of time.

        ByteBuffer b;
        if (toRead >= bufsize) {
            zeroCopy = true;
            b = ByteBuffer.wrap(readBuf);
            b.position(readPos);
        }
        else {
            zeroCopy = false;
            b = buf;
            b.clear();
        }
        return b;
    }

    //  Processes the data in the buffer previously allocated using
    //  get_buffer function. size_ argument specifies nemuber of bytes
    //  actually filled into the buffer. Function returns number of
    //  bytes actually processed.
    public int processBuffer(ByteBuffer buf, int size)
    {
        //  Check if we had an error in previous attempt.
        if (state() < 0) {
            return -1;
        }

        //  In case of zero-copy simply adjust the pointers, no copying
        //  is required. Also, run the state machine in case all the data
        //  were processed.
        if (zeroCopy) {
            readPos += size;
            toRead -= size;

            while (toRead == 0) {
                if (!next()) {
                    if (state() < 0) {
                        return -1;
                    }
                    return size;
                }
            }
            return size;
        }

        int pos = 0;
        while (true) {
            //  Try to get more space in the message to fill in.
            //  If none is available, return.
            while (toRead == 0) {
                if (!next()) {
                    if (state() < 0) {
                        return -1;
                    }

                    return pos;
                }
            }

            //  If there are no more data in the buffer, return.
            if (pos == size) {
                return pos;
            }

            //  Copy the data from buffer to the message.
            int toCopy = Math.min(toRead, size - pos);
            buf.get(readBuf, readPos, toCopy);
            readPos += toCopy;
            pos += toCopy;
            toRead -= toCopy;
        }
    }

    protected void nextStep(Msg msg, int state)
    {
        nextStep(msg.data(), msg.size(), state);
    }

    protected void nextStep(byte[] buf, int toRead, int state)
    {
        readBuf = buf;
        readPos = 0;
        this.toRead = toRead;
        this.state = state;
    }

    protected int state()
    {
        return state;
    }

    protected void state(int state)
    {
        this.state = state;
    }

    protected void decodingError()
    {
        state(-1);
    }

    //  Returns true if the decoder has been fed all required data
    //  but cannot proceed with the next decoding step.
    //  False is returned if the decoder has encountered an error.
    @Override
    public boolean stalled()
    {
        //  Check whether there was decoding error.
        if (!next()) {
            return false;
        }

        while (toRead == 0) {
            if (!next()) {
                if (!next()) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    protected abstract boolean next();
}
