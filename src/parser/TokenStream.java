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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleBooleanProperty;
import org.json.JSONObject;

/**
 *
 * @author Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel
 * <eda.ozge.eo at gmail.com>
 */
public class TokenStream {
    private JSONObject current = null;
    private List<String> keywords;
    private InputStream InputStream;
    
    public TokenStream(InputStream _InputStream) {
        keywords = new ArrayList<>(Arrays.asList("input",
                                                 "print",
                                                 "if",
                                                 "elseif",
                                                 "else",
                                                 "then",
                                                 "endif",
                                                 "for",
                                                 "to",
                                                 "by",
                                                 "do",
                                                 "endfor",
                                                 "while",
                                                 "endwhile",
                                                 "mod",
                                                 "not",
                                                 "and",
                                                 "or",
                                                 "abs",
                                                 "pow",
                                                 "sqrt",
                                                 "round",
                                                 "trunc",
                                                 "swap",
                                                 "return",
                                                 "algorithm",
                                                 "begin",
                                                 "endalgorithm",
                                                 "str",
                                                 "int",
                                                 "float",
                                                 "none"));
        this.InputStream = _InputStream;
    }
    
    public void reset() throws Exception {
        this.InputStream = new InputStream(this.InputStream._SourceFile);
        this.current = null;
    }
    
    public boolean AddKeyword(String input) {
        if (this.keywords.contains(input)) {
            return false;
        } else {
            this.keywords.add(input);
        }
        
        return true;
    }
    
    public boolean KeywordExists(String input) {
        return this.keywords.contains(input);
    }
    
    private boolean IsKeyword(String input) {
        return keywords.contains(input);
    }
    
    private boolean IsDigit(char ch) {
        return Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE).matcher(String.valueOf(ch)).find();
    }
    
    private boolean IsIdStart(char ch) {
        return Pattern.compile("[a-z_]", Pattern.CASE_INSENSITIVE).matcher(String.valueOf(ch)).find();
    }
    
    private Predicate<Character> IsId() {
        return ch -> (IsIdStart(ch) || "?!-<>=0123456789".contains(String.valueOf(ch)));
    }
    
    private Predicate<Character> IsOpChar() {
        return ch -> "+-*/!=<>".contains(String.valueOf(ch));
    }
    
    private boolean IsPunc(char ch) {
        return ",(){}[]".contains(String.valueOf(ch));
    }
    
    private Predicate<Character> IsWhitespace() {
        return ch -> " \t".contains(String.valueOf(ch));
    }
    
    private String ReadWhile(Predicate<Character> predicate) throws Exception {
        StringBuilder output = new StringBuilder();
        
        while(!InputStream.eof() && predicate.test(InputStream.peek())) {
            output.append(InputStream.next());
        }
        
        return output.toString();
    }
    
    private JSONObject ReadNumber() throws Exception {
        SimpleBooleanProperty HasDot = new SimpleBooleanProperty(false);
        JSONObject output = new JSONObject();
        
        String number = ReadWhile(ch -> {
            if(Objects.equals(ch, ".")) {
                if(HasDot.get())
                    return false;
                HasDot.set(true);
                return true;
            }
            return IsDigit(ch);
        });
        
        output.put("type", "num");
        output.put("value", number);
        
        return output;
    }
    
    private JSONObject ReadIdent() throws Exception {
        String id = ReadWhile(IsId());
        JSONObject output = new JSONObject();
        
        output.put("type", IsKeyword(id) ? "kw" : "var");
        output.put("value", id);
        
        return output;
    }
    
    private String ReadEscaped(char end) throws Exception {
        boolean escaped = false;
        StringBuilder output = new StringBuilder();
        InputStream.next();
        while(!InputStream.eof()) {
            char ch = InputStream.next();
            if(escaped) {
                output.append(ch);
                escaped = false;
            } else if("\\".equals(String.valueOf(ch))) {
                escaped = true;
            } else if(ch == end) {
                break;
            } else {
                output.append(ch);
            }
        }
        
        return output.toString();
    }
    
    private JSONObject ReadString() throws Exception {
        JSONObject output = new JSONObject();
        
        output.put("type", "str");
        output.put("value", ReadEscaped('"'));
        
        return output;
    }
    
    private void SkipComment() throws Exception {
        ReadWhile(ch -> {
            return !Objects.equals(ch, "\n");
        });
        InputStream.next();
    }
    
    private <T> T ReadNext() throws Exception {
        ReadWhile(IsWhitespace());
        if(InputStream.eof())
            return null;
        char ch = InputStream.peek();
        if(ch == '#') {
            SkipComment();
            return ReadNext();
        }
        if(ch == '\n') {
            JSONObject output = new JSONObject();
            output.put("type", "eol");
            output.put("value", "eol");
            InputStream.next();
            return (T) output;
        }
        if(ch == '"')
            return (T) ReadString();
        if(IsDigit(ch))
            return (T) ReadNumber();
        if(IsIdStart(ch))
            return (T) ReadIdent();
        if(IsPunc(ch)) {
            JSONObject output = new JSONObject();
            output.put("type", "punc");
            output.put("value", String.valueOf(InputStream.next()));
            return (T) output;
        }
        if(IsOpChar().test(ch)) {
            JSONObject output = new JSONObject();
            output.put("type", "op");
            output.put("value", ReadWhile(IsOpChar()));
            return (T) output;  
        }
        InputStream.ThrowException("Can't handle character: " + ch);
        return null;
    }
    
    public JSONObject Peek() throws Exception {
        return current != null ? current : (current = ReadNext());
    }
    
    public JSONObject Next() throws Exception {
        JSONObject token = current;
        current = null;
        return token != null ? token : ReadNext();
    }
    
    public boolean EOF() throws Exception {
        return Peek() == null;
    }
}
