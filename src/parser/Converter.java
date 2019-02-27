/*
 * The MIT License
 *
 * Copyright 2019 Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel <eda.ozge.eo at gmail.com>.
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

import java.io.File;
import java.util.HashMap;
import org.json.JSONObject;

/**
 *
 * @author Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel
 * <eda.ozge.eo at gmail.com>
 */
public class Converter {
    private TokenStream TokenStream;
    private File OutputFile;
    private JSONObject TempObject;
    private HashMap<String, String> SymbolTable;
    
    public Converter(TokenStream _TokenStream) {
        this.TokenStream = _TokenStream;
        OutputFile = null; // TODO
        TempObject = null;
        SymbolTable = new HashMap<>();
        // TODO
    }
    
    public void Convert() {
        
    }
    
    private String ProcessPrint() throws Exception {
        String output = "System.out.println(";
        TempObject = (JSONObject) TokenStream.ReadNext();
        if(!TempObject.get("type").equals("punc") && !TempObject.get("value").equals("(")) {
            throw new Exception();
        }
        TempObject = (JSONObject) TokenStream.ReadNext();
        if(TempObject.get("type").equals("var")) {
            output += ((String) TempObject.get("value")).substring(1, ((String) TempObject.get("value")).length() - 1) + ");";
        }
        else if(TempObject.get("type").equals("str")) {
            output += ((String) TempObject.get("value")) + ");";
        }
        else
            throw new Exception();
        return output;
    }
    
    private String ProcessAssignment(String var) throws Exception {
         String output = var + " = ";
         boolean doesExist = false;
        if(SymbolTable.containsKey(var)) {
          doesExist = true;
        } 
        TempObject = (JSONObject) TokenStream.ReadNext();
        while(!TempObject.get("type").equals("eol")){
            // Nested statements
            if(TempObject.get("value").equals("(")) {
                output += ProcessParantheses();
            }
            // Operators
            else if(TempObject.get("type").equals("op")) {
                String op = (String) TempObject.get("value");
                switch (op) {
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                        output += op;
                        break;
                    case "!":
                        if(TokenStream.Peek().get("value").equals("=")) {
                            output += "!=";
                            TokenStream.Next();
                        } else {
                            throw new Exception();
                        }
                        break;
                    case "=":
                        if(TokenStream.Peek().get("value").equals("=")) {
                            output += "==";
                            TokenStream.Next();
                        } else {
                            throw new Exception();
                        }
                        break;
                    case "<":
                    case ">":
                        output += op;
                        if(TokenStream.Peek().get("value").equals("=")) {
                            output += "=";
                            TokenStream.Next();
                        }
                        break;
                    default:
                        throw new Exception();
                }
            }
            // Keywords
            else if(TempObject.get("type").equals("kw"))
                output += ProcessKeywords();
            // Variables and numbers
            else if(TempObject.get("type").equals("var") || TempObject.get("type").equals("num")) {
                output += ((String) TempObject.get("value"));
            }
            // Strings
            else if(TempObject.get("type").equals("str")) {
                if(!doesExist){
                    SymbolTable.put(var, "str");
                    output = "String " + output;
                } else if(!SymbolTable.get(var).equals("str")) {
                    throw new Exception();
                }
                output += "\"" + ((String) TempObject.get("value")) + "\"";
 
            } else {
                throw new Exception();
            }
            TempObject = (JSONObject) TokenStream.ReadNext();  
        }
        
        return output + ";";
    }
    
    private String ProcessParantheses() {
        // TODO
        return null;
    }
    
    private String ProcessKeywords() {
        // TODO
        return null;
    }
}
