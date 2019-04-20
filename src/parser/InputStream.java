/*
 * The MIT License
 *
 * Copyright 2018 Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel <eda.ozge.eo at gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package parser;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel
 * <eda.ozge.eo at gmail.com>
 */
public class InputStream {
    private int pos;
    private int line;
    private int col;
    RandomAccessFile SourceCodeFile;
    File _SourceFile;
    
    public InputStream(File _SourceCodeFile) throws Exception {
        pos = 0;
        line = 1;
        col = 0;
        try {
            this.SourceCodeFile = new RandomAccessFile(_SourceCodeFile, "r");
        } catch (FileNotFoundException e) {
            ThrowException("file can't be opened", e);
        }
        _SourceFile = _SourceCodeFile.getAbsoluteFile();
    }
    
    public char next() throws Exception {
        char ch = 0;
        
        try {
            ch = (char) SourceCodeFile.readByte();
        } catch (IOException e) {
            ThrowException("char can't be read", e);
        }
        
        pos++;
        
        if (ch == '\n') {
            line++;
            col = 0;
        } else
            col++;
        
        return ch;
    }
    
    public char peek() throws Exception {
        char ch = 0;
        
        try {
            ch = (char) SourceCodeFile.readByte();
        } catch (EOFException e) {
            throw e;
        } catch (IOException e) {
            ThrowException("char can't be read", e);
        }
        try {
            SourceCodeFile.seek(pos);
        } catch (IOException e) {
            ThrowException("char pos can't be set", e);
        }
        
        return ch;
    }
    
    public boolean eof() throws Exception {
        try {
            peek();
        } catch (EOFException e) {
            return true;
        }
        
        return false;
    }
    
    private void ThrowException(String message, Throwable cause) throws Exception {
        throw new Exception(message + " (" + getLine() + ":" + col + ")", cause);
    }
    
    public void ThrowException(String message) throws Exception {
        throw new Exception(message + " (" + getLine() + ":" + col + ")");
    }

    /**
     * @return the line
     */
    public int getLine() {
        return line;
    }
}
