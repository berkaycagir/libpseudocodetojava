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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import com.google.googlejavaformat.java.Formatter;

/**
 *
 * @author Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel
 * <eda.ozge.eo at gmail.com>
 */
public class Converter {

    private InputStream InputStream;
    private TokenStream TokenStream;
    private String FunctionReturnType;
    private boolean FunctionReturnSatisfied;
    private File OutputFile;
    private List<String> types = Arrays.asList("str", "int", "float", "none", "List");
    private HashMap<String, String> SymbolTable = new HashMap<>();
    private HashMap<String, String> ArrayTable = new HashMap<>();
    private HashMap<String, List<String>> FunctionTable = new HashMap<String, List<String>>() {
        {
            put("abs", Arrays.asList("int", "float"));
            put("pow", Arrays.asList(""));
            put("sqrt", Arrays.asList(""));
            put("round", Arrays.asList("int"));
            put("trunc", Arrays.asList(""));
        }
    };

    public Converter(String FilePath) throws Exception {
        this.InputStream = new InputStream(new File(FilePath));
        this.TokenStream = new TokenStream(this.InputStream);
        OutputFile = null; // TODO
    }

    public void Convert(String FilePath) throws Exception {
        List<String> array = new ArrayList<>();
        array.add("public class Pseudocode {");
        ProcessMethods();
        JSONObject TempObject = TokenStream.Next();
        Path file = Paths.get(FilePath);
        String line = "";
        String tip = "";
        boolean isList = false;
        while (!TokenStream.EOF()) {
            while (TempObject.getString("type").equals("eol")) {
                TempObject = TokenStream.Next();
            }
            if (TempObject.getString("value").equals("algo")) {
                line = "public static ";
                // type
                TempObject = TokenStream.Next();
                if (!types.contains(TempObject.getString("value"))) {
                    throw new Exception("Undefined type on line: " + TokenStream.GetCurrentLine());
                }
                FunctionReturnType = TempObject.getString("value");
                line += (FunctionReturnType.equals("none")) ? "void " : (FunctionReturnType.equals("str")) ? "String " : FunctionReturnType + " ";
                // method name
                TempObject = TokenStream.Next();
                if (!FunctionTable.containsKey(TempObject.getString("value"))) {
                    throw new Exception("Undefined method on line: " + TokenStream.GetCurrentLine());
                } else {
                    line += TempObject.getString("value");
                }
                // paranthesis opening
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("(")) {
                    throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                }
                line += TempObject.getString("value");
                // parameters
                TempObject = TokenStream.Next();
                HashMap<String, String> localVar = new HashMap<>();
                while (!TempObject.getString("value").equals(")")) {
                    if (!types.contains(TempObject.getString("value"))) {
                        throw new Exception("Undefined type on line: " + TokenStream.GetCurrentLine());
                    }
                    tip = TempObject.getString("value");
                    TempObject = TokenStream.Next();
                    if (TempObject.getString("value").equals("[")) {
                        TempObject = TokenStream.Next();
                        if (!TempObject.getString("value").equals("]")) {
                            throw new Exception("Missing ] on line: " + TokenStream.GetCurrentLine());
                        }
                        TempObject = TokenStream.Next();
                        isList = true;
                    }
                    line += ((tip.equals("str")) ? "String" : tip) + ((isList) ? "[]" : "") + " ";

                    if (!TempObject.getString("type").equals("var")) {
                        throw new Exception("Missing variable on line: " + TokenStream.GetCurrentLine());
                    }
                    if (localVar.containsKey(TempObject.getString("value"))) {
                        throw new Exception("Variable already exists on line: " + TokenStream.GetCurrentLine());
                    }
                    if (!isList) {
                        localVar.put(TempObject.getString("value"), tip);
                        SymbolTable.put(TempObject.getString("value"), tip);
                    } else {
                        localVar.put(TempObject.getString("value"), "list");
                        SymbolTable.put(TempObject.getString("value"), "list");
                        ArrayTable.put(TempObject.getString("value"), tip);
                    }
                    line += TempObject.getString("value");
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("value").equals(",") && !TempObject.getString("value").equals(")")) {
                        throw new Exception("Missing comma or paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    line += (TempObject.getString("value").equals(",")) ? ", " : "";
                    if (TempObject.getString("value").equals(",")) {
                        TempObject = TokenStream.Next();
                    }
                    isList = false;
                }
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("begin")) {
                    throw new Exception("Missing keyword \"begin\" on line: " + TokenStream.GetCurrentLine());
                }
                line += ") {";
                TempObject = TokenStream.Next();
                if (!TempObject.getString("type").equals("eol")) {
                    throw new Exception("Missing end of line on line: " + TokenStream.GetCurrentLine());
                }
                TempObject = TokenStream.Next();
                array.add(line);
                FunctionReturnSatisfied = false;
                while (!TempObject.getString("value").equals("endalgo")) {
                    array.addAll(ProcessType(TempObject));
                    TempObject = TokenStream.Next();
                    while (TempObject.getString("type").equals("eol")) {
                        TempObject = TokenStream.Next();
                    }
                }
                if (!FunctionReturnType.equals("none")
                        && !FunctionReturnSatisfied) {
                    throw new Exception("Function didn't return on line: " + TokenStream.GetCurrentLine());
                }
                localVar.clear();
                SymbolTable.clear();
                ArrayTable.clear();

                FunctionReturnType = "";
                FunctionReturnSatisfied = false;
                line = "}";
                array.add(line);
            }
            if (!TokenStream.EOF()) {
                TempObject = TokenStream.Next();
            }
        }
        array.add("}");
        Files.write(file, Arrays.asList(new Formatter().formatSource(String.join("\n", array))), Charset.forName("UTF-8"));
    }

    private void ProcessMethods() throws Exception {
        String name, type;
        JSONObject TempObject = TokenStream.Next();
        while (!TokenStream.EOF()) {
            while (TempObject.getString("type").equals("eol")) {
                TempObject = TokenStream.Next();
            }
            if (TempObject.getString("value").equals("algo")) {
                // type
                TempObject = TokenStream.Next();
                if (!types.contains(TempObject.getString("value"))) {
                    throw new Exception("Unknown type on line: " + TokenStream.GetCurrentLine());
                }
                type = TempObject.getString("value");
                // method name
                TempObject = TokenStream.Next();
                if (TokenStream.KeywordExists(TempObject.getString("value"))) {
                    throw new Exception("Method named as a keyword on line: " + TokenStream.GetCurrentLine());
                } else {
                    name = TempObject.getString("value");
                }
                // paranthesis opening
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("(")) {
                    throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                }
                // parameters
                TempObject = TokenStream.Next();
                while (!TempObject.getString("value").equals(")")) {
                    if (!types.contains(TempObject.getString("value"))) {
                        throw new Exception("Unknown type on line: " + TokenStream.GetCurrentLine());
                    }
                    TempObject = TokenStream.Next();
                    if (TempObject.getString("value").equals("[")) {
                        TempObject = TokenStream.Next();
                        if (!TempObject.getString("value").equals("]")) {
                            throw new Exception("Missing ] on line: " + TokenStream.GetCurrentLine());
                        }
                        TempObject = TokenStream.Next();
                    }
                    if (!TempObject.getString("type").equals("var")) {
                        throw new Exception("Missing variable on line: " + TokenStream.GetCurrentLine());
                    }
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("value").equals(",") && !TempObject.getString("value").equals(")")) {
                        throw new Exception("Missing comma or paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    if (TempObject.getString("value").equals(",")) {
                        TempObject = TokenStream.Next();
                    }
                }
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("begin")) {
                    throw new Exception("Missing keyword \"begin\" on line: " + TokenStream.GetCurrentLine());
                }
                TempObject = TokenStream.Next();
                boolean endAlg = false;
                while (!TokenStream.EOF()) {
                    TempObject = TokenStream.Next();
                    if (TempObject.getString("value").equals("endalgo")) {
                        endAlg = true;
                        break;
                    }
                }
                if (!endAlg) {
                    throw new Exception("Missing keyword \"endalgorithm\" on line: " + TokenStream.GetCurrentLine());
                }
                TokenStream.AddKeyword(name);
                FunctionTable.put(name, Arrays.asList(type));
            }
            TempObject = TokenStream.Next();
        }
        TokenStream.reset();
    }

    private String ProcessArrayIndice() throws Exception {
        JSONObject TempObject = TokenStream.Next();
        if (TempObject.getString("value").equals("]")) {
            throw new Exception("Prematurely closed [ on line: " + TokenStream.GetCurrentLine());
        }
        String indice = "";
        while (!TempObject.getString("value").equals("]")) {
            switch (TempObject.getString("type")) {
                case "var":
                    if (!SymbolTable.containsKey(TempObject.getString("value"))
                            || !SymbolTable.get(TempObject.getString("value")).equals("int")) {
                        throw new Exception("Not a variable or not an integer on line: " + TokenStream.GetCurrentLine());
                    }
                    indice += TempObject.getString("value") + " ";
                    break;
                case "kw":
                    if (!FunctionTable.containsKey(TempObject.getString("value"))
                            || (!FunctionTable.get(TempObject.getString("value")).contains("int")
                            && !FunctionTable.get(TempObject.getString("value")).get(0).equals(""))) {
                        throw new Exception("Not a function or does not return integer on line: " + TokenStream.GetCurrentLine());
                    }
                    String FunctionName = TempObject.getString("value");
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("value").equals("(")) {
                        throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    if (FunctionTable.get(FunctionName).get(0).equals("")) {
                        indice += "(int) ";
                    }
                    switch (FunctionName) {
                        case "abs":
                            indice += "Math.abs";
                            break;
                        case "pow":
                            indice += "Math.pow";
                            break;
                        case "sqrt":
                            indice += "Math.sqrt";
                            break;
                        case "round":
                            indice += "Math.round";
                            break;
                        case "trunc":
                            indice += "Math.ceil";
                            break;
                        default:
                            indice += FunctionName;
                    }
                    indice += ProcessParantheses() + " ";
                    break;
                case "num":
                    if (!TempObject.getString("subtype").equals("int")) {
                        throw new Exception("Wrong number type on line: " + TokenStream.GetCurrentLine());
                    }
                    indice += TempObject.getString("value") + " ";
                    break;
                case "punc":
                    if (!TempObject.getString("value").equals("(")) {
                        throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    indice += ProcessParantheses() + " ";
                    break;
                case "op":
                    if (!"+-*/".contains(TempObject.getString("value"))) {
                        throw new Exception("Unknown operator on line: " + TokenStream.GetCurrentLine());
                    }
                    if (TokenStream.Peek().getString("value").equals("]")) {
                        throw new Exception("Prematurely closed [ on line: " + TokenStream.GetCurrentLine());
                    }
                    indice += TempObject.getString("value") + " ";
                    break;
                default:
                    throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
            }
            TempObject = TokenStream.Next();
        }
        return indice.substring(0, indice.length() - 1);
    }

    private List<String> ProcessType(JSONObject TempObject) throws Exception {
        List<String> Output = new ArrayList<>();
        switch (TempObject.getString("type")) {
            case "num":
                Output.add(TempObject.getString("value") + " ");
                break;
            case "var":
                String var = TempObject.getString("value");
                // Normal assignments
                if (TokenStream.Peek().getString("value").equals("=")) {
                    TokenStream.Next();
                    Output.add(ProcessAssignment(var, false, null));
                } // Array assignments
                else if (TokenStream.Peek().getString("value").equals("[")) {
                    if (!SymbolTable.get(var).equals("list")) {
                        throw new Exception("Not an array on line: " + TokenStream.GetCurrentLine());
                    }
                    TokenStream.Next();
                    String indice = ProcessArrayIndice();
                    if (!(TempObject = TokenStream.Next()).getString("value").equals("=")) {
                        throw new Exception("Missing = on line: " + TokenStream.GetCurrentLine());
                    }
                    Output.add(ProcessAssignment(var, true, indice));
                } // No assignments?
                else {
                    Output.add(TempObject.getString("value") + " ");
                }
                break;
            case "kw":
                Output.addAll(ProcessKeywords(TempObject));
                break;
            case "str":
                // GEREKSIZ OLABILIR (EDA DEDI ONA KIZIN)
                Output.add("\"" + TempObject.getString("value") + "\"");
                break;
            case "eol":
                // TODO
                /* if(!TokenStream.EOF() && TokenStream.Peek().getString("type").equals("eol")){
                    Output.add("\n");   
                } */
                break;
            case "punc":
                if (TempObject.getString("value").equals("(")) {
                    Output.add(ProcessParantheses());
                }
                Output.add(TempObject.getString("value"));
                break;
            case "op":
                Output.add(TempObject.getString("value"));
                break;
        }
        return Output;
    }

    private String ProcessPrint() throws Exception {
        String output = "System.out.println(";
        boolean ContainsString = false;
        JSONObject TempObject = TokenStream.Next();
        if (!TempObject.getString("type").equals("punc") && !TempObject.getString("value").equals("(")) {
            throw new Exception("Not a punctuation or paranthesis on line: " + TokenStream.GetCurrentLine());
        }
        if ((TempObject = TokenStream.Next()).getString("value").equals(")")) {
            return output + ");";
        }
        while (!TempObject.getString("value").equals(")")) {
            switch (TempObject.getString("type")) {
                case "var":
                    if (TokenStream.Peek().getString("value").equals("[")) {
                        if (!SymbolTable.get(TempObject.getString("value")).equals("list")) {
                            throw new Exception("Not an array on line: " + TokenStream.GetCurrentLine());
                        }
                        TokenStream.Next();
                        output += TempObject.getString("value") + "[" + ProcessArrayIndice() + "]";
                    } else {
                        if (!SymbolTable.containsKey(TempObject.getString("value"))) {
                            throw new Exception("Variable \"" + TempObject.getString("value") + "\" does not exist on line: " + TokenStream.GetCurrentLine());
                        }
                        output += TempObject.getString("value");
                    }
                    break;
                case "str":
                    output += "\"" + TempObject.getString("value") + "\"";
                    if (!ContainsString) {
                        ContainsString = true;
                    }
                    break;
                case "num":
                    output += TempObject.getString("value");
                    break;
                case "op":
                    // not complete
                    if (!"+-*/".contains(TempObject.getString("value"))) {
                        throw new Exception("Unknown operator on line: " + TokenStream.GetCurrentLine());
                    }
                    if (ContainsString && !TempObject.getString("value").equals("+")) {
                        throw new Exception("Non-applicable operator on strings on line: " + TokenStream.GetCurrentLine());
                    }
                    output += TempObject.getString("value");
                    break;
                case "punc":
                    if (TempObject.getString("value").equals("(")) {
                        output += ProcessParantheses();
                    } else {
                        throw new Exception("Non-applicable punctuation on line: " + TokenStream.GetCurrentLine());
                    }
                    break;
                default:
                    throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
            }
            TempObject = TokenStream.Next();
        }
        TokenStream.Next();
        return output + ");";
    }

    // TODO
    private String ProcessAssignment(String var, boolean IsArrayAssignment, String indice) throws Exception {
        String output = var + ((IsArrayAssignment) ? "[" + indice + "]" : "") + " = ";
        boolean doesExist = SymbolTable.containsKey(var);
        boolean typeAssigned = false;
        String NumberType = "";
        JSONObject TempObject = TokenStream.Next();
        while (!TempObject.getString("type").equals("eol")) {
            // Nested statements
            if (TempObject.getString("value").equals("(")) {
                output += ProcessParantheses();
            } // Operators
            else if (TempObject.getString("type").equals("op")) {
                if (TokenStream.Peek().getString("value").equals("eol")) {
                    throw new Exception("Premature end of line on line: " + TokenStream.GetCurrentLine());
                }
                String op = TempObject.getString("value");
                switch (op) {
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                        output += " " + op + " ";
                        break;
                    case "!":
                        if (TokenStream.Peek().get("value").equals("=")) {
                            output += "!=";
                            TokenStream.Next();
                        } else {
                            throw new Exception("! without = on line: " + TokenStream.GetCurrentLine());
                        }
                        break;
                    case "=":
                        if (TokenStream.Peek().get("value").equals("=")) {
                            output += "==";
                            TokenStream.Next();
                        } else {
                            throw new Exception("= without = on line: " + TokenStream.GetCurrentLine());
                        }
                        break;
                    case "<":
                    case ">":
                        output += op;
                        if (TokenStream.Peek().get("value").equals("=")) {
                            output += "=";
                            TokenStream.Next();
                        }
                        break;
                    default:
                        throw new Exception("Undefined keyword on line: " + TokenStream.GetCurrentLine());
                }
            } // Keywords
            else if (TempObject.getString("type").equals("kw")) {
                // type assignment eksik
                if (TempObject.getString("value").equals("mod")) {
                    output += " % ";
                } else {
                    if (!FunctionTable.containsKey(TempObject.getString("value"))) {
                        throw new Exception("Not an assignable keyword on line: " + TokenStream.GetCurrentLine());
                    }
                    
                    if (FunctionTable.get(TempObject.getString("value")).get(0).equals("none")) {
                        throw new Exception("Assigned function does not return a value on line: " + TokenStream.GetCurrentLine());
                    }
                    
                    if (IsArrayAssignment) {
                        if (!FunctionTable.get(TempObject.getString("value")).contains(ArrayTable.get(var))) {
                            throw new Exception("Mismatching function return and array types on line: " + TokenStream.GetCurrentLine());
                        }
                        output += ProcessKeywords(TempObject).get(0);
                    } else {
                        // if left hand variable exists
                        if (doesExist) {
                            // but not of the same type
                            if ((SymbolTable.get(var).equals("str") || FunctionTable.get(TempObject.getString("value")).get(0).equals("str"))
                                    && !SymbolTable.get(var).equals(FunctionTable.get(TempObject.getString("value")).get(0))) {
                                throw new Exception("Mismatching types on line: " + TokenStream.GetCurrentLine());
                            }
                            if (NumberType.equals("int")
                                    && FunctionTable.get(TempObject.getString("value")).get(0).equals("float")) {
                                output = "float" + output.substring(3, output.length());
                                SymbolTable.put(var, "float");
                                NumberType = "float";
                            }
                        } // if left hand variable does not exist and has no type assigned
                        else if (!doesExist && !typeAssigned) {
                            // assign the responding type
                            switch (FunctionTable.get(TempObject.getString("value")).get(0)) {
                                case "int":
                                case "float":
                                    output = FunctionTable.get(TempObject.getString("value")).get(0) + " " + output;
                                    NumberType = FunctionTable.get(TempObject.getString("value")).get(0);
                                    break;
                                case "str":
                                    output = "String " + output;
                                    break;
                                default:
                                    throw new Exception("Undefined type on line: " + TokenStream.GetCurrentLine());
                            }
                            typeAssigned = true;
                            doesExist = true;
                            SymbolTable.put(var, FunctionTable.get(TempObject.getString("value")).get(0));
                        }
                        output += ProcessKeywords(TempObject).get(0);
                    }
                }
            } // Variables
            else if (TempObject.getString("type").equals("var")) {
                boolean IsArrayOperand = TokenStream.Peek().getString("value").equals("[");
                // if right hand variable exists
                if (SymbolTable.containsKey(TempObject.getString("value"))) {
                    // Array operand
                    if (IsArrayOperand) {
                        String ArrayName = TempObject.getString("value");
                        if (!SymbolTable.get(ArrayName).equals("list")) {
                            throw new Exception("Not an array on line: " + TokenStream.GetCurrentLine());
                        }
                        TokenStream.Next();
                        String operand = ArrayName + "[" + ProcessArrayIndice() + "]";
                        if (IsArrayAssignment) {
                            if (!ArrayTable.get(var).equals(ArrayTable.get(ArrayName))) {
                                throw new Exception("Mismatching array types on line: " + TokenStream.GetCurrentLine());
                            }
                            output += operand;
                        } else {
                            // if left hand variable exists
                            if (doesExist) {
                                // but not of the same type
                                if ((SymbolTable.get(var).equals("str") || ArrayTable.get(ArrayName).equals("str"))
                                        && !SymbolTable.get(var).equals(ArrayTable.get(ArrayName))) {
                                    throw new Exception("Mismatching array and variable types on line: " + TokenStream.GetCurrentLine());
                                }
                                if (NumberType.equals("int")
                                        && ArrayTable.get(ArrayName).equals("float")) {
                                    // TODO 1
                                    output = "float" + output.substring(3, output.length());
                                    SymbolTable.put(var, "float");
                                    NumberType = "float";
                                }
                            } // if left hand variable does not exist and has no type assigned
                            else if (!doesExist && !typeAssigned) {
                                // assign the responding type
                                switch (ArrayTable.get(ArrayName)) {
                                    case "int":
                                    case "float":
                                        output = ArrayTable.get(ArrayName) + " " + output;
                                        NumberType = ArrayTable.get(ArrayName);
                                        break;
                                    case "str":
                                        output = "String " + output;
                                        break;
                                    default:
                                        throw new Exception("Undefined type on line: " + TokenStream.GetCurrentLine());
                                }
                                typeAssigned = true;
                                doesExist = true;
                                SymbolTable.put(var, ArrayTable.get(ArrayName));
                            }
                            output += operand;
                        }
                    } // Variable operand
                    else {
                        if (IsArrayAssignment) {
                            if (!ArrayTable.get(var).equals(SymbolTable.get(TempObject.getString("value")))) {
                                throw new Exception("Mismatching variable and array types on line: " + TokenStream.GetCurrentLine());
                            }
                            output += TempObject.getString("value");
                        } else {
                            // if left hand variable exists
                            if (doesExist) {
                                // but not of the same type
                                if ((SymbolTable.get(var).equals("str") || SymbolTable.get(TempObject.getString("value")).equals("str"))
                                        && !SymbolTable.get(var).equals(SymbolTable.get(TempObject.getString("value")))) {
                                    throw new Exception("Mismatching types on line: " + TokenStream.GetCurrentLine());
                                }
                                if (NumberType.equals("int")
                                        && SymbolTable.get(TempObject.getString("value")).equals("float")) {
                                    output = "float" + output.substring(3, output.length());
                                    SymbolTable.put(var, "float");
                                    NumberType = "float";
                                }
                            } // if left hand variable does not exist and has no type assigned
                            else if (!doesExist && !typeAssigned) {
                                // assign the responding type
                                switch (SymbolTable.get(TempObject.getString("value"))) {
                                    case "int":
                                    case "float":
                                        output = SymbolTable.get(TempObject.getString("value")) + " " + output;
                                        NumberType = SymbolTable.get(TempObject.getString("value"));
                                        break;
                                    case "str":
                                        output = "String " + output;
                                        break;
                                    default:
                                        throw new Exception("Undefined type on line: " + TokenStream.GetCurrentLine());
                                }
                                typeAssigned = true;
                                doesExist = true;
                                SymbolTable.put(var, SymbolTable.get(TempObject.getString("value")));
                            }
                            output += TempObject.getString("value");
                        }
                    }
                } // if variable does not exist
                else {
                    throw new Exception("Variable does not exist on line: " + TokenStream.GetCurrentLine());
                }
            } // Numbers
            else if (TempObject.getString("type").equals("num")) {
                if (IsArrayAssignment) {
                    if (ArrayTable.get(var).equals("int")
                            && TempObject.getString("subtype").equals("float")) {
                        output += "(int) ";
                    } else if (ArrayTable.get(var).equals("float")
                            && TempObject.getString("subtype").equals("int")) {
                        output += "(float) ";
                    } else if (!ArrayTable.get(var).equals(TempObject.getString("subtype"))) {
                        throw new Exception("Mismatching types on line: " + TokenStream.GetCurrentLine());
                    }
                } else {
                    if (TempObject.getString("subtype").equals("float")) {
                        if (!doesExist) {
                            SymbolTable.put(var, "float");
                            output = "float " + output;
                        } else if (SymbolTable.get(var).equals("str")) {
                            throw new Exception("Mismatching type on line: " + TokenStream.GetCurrentLine());
                        }
                    } else {
                        if (!doesExist) {
                            SymbolTable.put(var, "int");
                            output = "int " + output;
                        } else if (SymbolTable.get(var).equals("str")) {
                            throw new Exception("Mismatching type on line: " + TokenStream.GetCurrentLine());
                        }
                    }
                }
                output += TempObject.getString("value") + ((TempObject.getString("subtype").equals("float")) ? "f" : "");
            } // Strings
            else if (TempObject.getString("type").equals("str")) {
                if (IsArrayAssignment) {
                    if (!ArrayTable.get(var).equals(TempObject.getString("type"))) {
                        throw new Exception("Mismatching types on line: " + TokenStream.GetCurrentLine());
                    }
                } else {
                    if (!doesExist) {
                        SymbolTable.put(var, "str");
                        output = "String " + output;
                    } else if (!SymbolTable.get(var).equals("str")) {
                        throw new Exception("Mismatching types on line: " + TokenStream.GetCurrentLine());
                    }
                }
                output += "\"" + TempObject.getString("value") + "\"";
            } // Lists
            else if (TempObject.getString("type").equals("punc")) {
                if (!TempObject.getString("value").equals("[")) {
                    throw new Exception("Missing [ on line: " + TokenStream.GetCurrentLine());
                }
                TempObject = TokenStream.Next();
                if (TempObject.getString("value").equals("]")) {
                    throw new Exception("Premature closing of [ on line: " + TokenStream.GetCurrentLine());
                }
                if (!types.contains(TempObject.getString("value"))
                        || TempObject.getString("value").equals("none")) {
                    throw new Exception("Unknown type on line: " + TokenStream.GetCurrentLine());
                }
                String arrayType = TempObject.getString("value");
                if (doesExist) {
                    if (!SymbolTable.get(var).equals("list")
                            || !ArrayTable.get(var).equals(arrayType)) {
                        throw new Exception("Not a list or unmatching types on line: " + TokenStream.GetCurrentLine());
                    }
                } else {
                    output = ((arrayType.equals("str") ? "String" : arrayType))
                            + "[] " + output;
                }
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals(",")) {
                    throw new Exception("Missing comma on line: " + TokenStream.GetCurrentLine());
                }
                TempObject = TokenStream.Next();
                if (TempObject.getString("value").equals("]")) {
                    throw new Exception("Premature closing of [ on line: " + TokenStream.GetCurrentLine());
                }
                String size = "";
                // TODO
                while (!TempObject.getString("value").equals("]")) {
                    switch (TempObject.getString("type")) {
                        case "kw":
                            if (!FunctionTable.containsKey(TempObject.getString("value"))
                                    || (!FunctionTable.get(TempObject.getString("value")).contains("int")
                                    && !FunctionTable.get(TempObject.getString("value")).get(0).equals(""))) {
                                throw new Exception("Unknown keyword or unmatching types on line: " + TokenStream.GetCurrentLine());
                            }
                            String FunctionName = TempObject.getString("value");
                            TempObject = TokenStream.Next();
                            if (!TempObject.getString("value").equals("(")) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            if (FunctionTable.get(FunctionName).get(0).equals("")) {
                                size += "(int) ";
                            }
                            switch (FunctionName) {
                                case "abs":
                                    size += "Math.abs";
                                    break;
                                case "pow":
                                    size += "Math.pow";
                                    break;
                                case "sqrt":
                                    size += "Math.sqrt";
                                    break;
                                case "round":
                                    size += "Math.round";
                                    break;
                                case "trunc":
                                    size += "Math.ceil";
                                    break;
                                default:
                                    size += FunctionName;
                            }
                            size += ProcessParantheses() + " ";
                            break;
                        case "num":
                            if (!TempObject.getString("subtype").equals("int")) {
                                throw new Exception("Non-integer on line: " + TokenStream.GetCurrentLine());
                            }
                            size += TempObject.getString("value") + " ";
                            break;
                        case "op":
                            if (!"+-*/".contains(TempObject.getString("value"))) {
                                throw new Exception("Unknown operator on line: " + TokenStream.GetCurrentLine());
                            }
                            if (TokenStream.Peek().getString("value").equals("]")) {
                                throw new Exception("Premature closing of [ on line: " + TokenStream.GetCurrentLine());
                            }
                            size += TempObject.getString("value") + " ";
                            break;
                        case "punc":
                            if (!TempObject.getString("value").equals("(")) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            size += ProcessParantheses() + " ";
                            break;
                        default:
                            throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
                    }
                    TempObject = TokenStream.Next();
                }
                ArrayTable.put(var, arrayType);
                if (!doesExist) {
                    SymbolTable.put(var, "list");
                }
                output += "new "
                        + ((arrayType.equals("str") ? "String" : arrayType))
                        + "[" + size.substring(0, size.length() - 1) + "]";
            } else {
                throw new Exception("Unknown type on line: " + TokenStream.GetCurrentLine());
            }
            TempObject = TokenStream.Next();
        }

        return output + ";";
    }

    private List<String> ProcessIf() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "if ";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (TempObject.getString("value").equals("(")) {
            line += ProcessParantheses() + " {";
        } else {
            throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
        }
        output.add(line);
        line = "";
        // Confirm then exists
        if (!(TempObject = TokenStream.Next()).get("value").equals("then")) {
            throw new Exception("Missing \"then\" on line: " + TokenStream.GetCurrentLine());
        }
        // Process body
        TempObject = TokenStream.Next();
        while (TempObject.getString("type").equals("eol")) {
            TempObject = TokenStream.Next();
        }
        while (!TempObject.getString("type").equals("eol")
                && !TempObject.getString("value").equals("else")
                && !TempObject.getString("value").equals("elseif")
                && !TempObject.getString("value").equals("endif")) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
        }
        output.add("}");
        // elseif
        while (TempObject.getString("type").equals("eol")) {
            TempObject = TokenStream.Next();
        }
        while (!TempObject.getString("type").equals("eol")
                && !TempObject.getString("value").equals("endif")) {
            switch (TempObject.getString("value")) {
                case "elseif":
                    while (TempObject.getString("value").equals("elseif")) {
                        line = "else if ";
                        TempObject = TokenStream.Next();
                        // Condition
                        if (TempObject.getString("value").equals("(")) {
                            line += ProcessParantheses() + " {";
                        } else {
                            throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                        }
                        output.add(line);
                        line = "";

                        if (!(TempObject = TokenStream.Next()).get("value").equals("then")) {
                            throw new Exception("Missing \"then\" on line: " + TokenStream.GetCurrentLine());
                        }

                        TempObject = TokenStream.Next();
                        while (TempObject.getString("type").equals("eol")) {
                            TempObject = TokenStream.Next();
                        }
                        while (!TempObject.getString("type").equals("eol")
                                && !TempObject.getString("value").equals("else")
                                && !TempObject.getString("value").equals("elseif")
                                && !TempObject.getString("value").equals("endif")) {
                            output.addAll(ProcessType(TempObject));
                            TempObject = TokenStream.Next();
                        }
                        output.add("}");
                    }
                    break;
                case "else":
                    line = "else {";
                    TempObject = TokenStream.Next();
                    if ((TempObject = TokenStream.Next()).get("value").equals("then")) {
                        throw new Exception("Premature \"then\" on line: " + TokenStream.GetCurrentLine());
                    }
                    output.add(line);
                    line = "";

                    while (TempObject.getString("type").equals("eol")) {
                        TempObject = TokenStream.Next();
                    }
                    while (!TempObject.getString("type").equals("eol")
                            && !TempObject.getString("value").equals("else")
                            && !TempObject.getString("value").equals("elseif")
                            && !TempObject.getString("value").equals("endif")) {
                        output.addAll(ProcessType(TempObject));
                        TempObject = TokenStream.Next();
                    }
                    if (!TempObject.getString("value").equals("endif")) {
                        throw new Exception("Missing \"endif\" on line: " + TokenStream.GetCurrentLine());
                    }
                    output.add("}");
                    break;
            }
        }
        return output;
    }

    private List<String> ProcessWhile() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "while ";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (TempObject.getString("value").equals("(")) {
            line += ProcessParantheses() + " {";
        } else {
            throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
        }
        output.add(line);
        line = "";
        // Confirm do exists
        if (!(TempObject = TokenStream.Next()).get("value").equals("do")) {
            throw new Exception("Missing \"do\" on line: " + TokenStream.GetCurrentLine());
        }
        // Process body
        TempObject = TokenStream.Next();
        while (TempObject.getString("type").equals("eol")) {
            TempObject = TokenStream.Next();
        }
        while (!TempObject.getString("value").equals("endwhile")) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
            while (TempObject.getString("type").equals("eol")) {
                TempObject = TokenStream.Next();
            }
        }
        output.add("}");
        return output;
    }

    private List<String> ProcessFor() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "for (";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (!TempObject.getString("type").equals("var")) {
            throw new Exception("Mismatching types on line: " + TokenStream.GetCurrentLine());
        }
        String var = TempObject.getString("value");
        // Check if var exists (and has a compatible value)
        boolean TempVariable = !SymbolTable.containsKey(var);
        if (SymbolTable.containsKey(var)) {
            if (SymbolTable.get(var).equals("list")) {
                if (!TokenStream.Peek().getString("value").equals("[")) {
                    throw new Exception("Missing [ on line: " + TokenStream.GetCurrentLine());
                }
                if (!ArrayTable.get(var).equals("int")) {
                    throw new Exception("Not an integer on line: " + TokenStream.GetCurrentLine());
                }
                TokenStream.Next();
                var = var + "[" + ProcessArrayIndice() + "]";
                line += var + " = ";
            } else {
                if (SymbolTable.get(var).equals("int")) {
                    line += var + " = ";
                } else {
                    throw new Exception("Not an integer on line: " + TokenStream.GetCurrentLine());
                }
            }
        } else {
            line += "int " + var + " = ";
            SymbolTable.put(var, "int");
        }

        // =
        if (!(TempObject = TokenStream.Next()).getString("value").equals("=")) {
            throw new Exception("Missing = on line: " + TokenStream.GetCurrentLine());
        }

        // baslangic_degeri
        if ((TempObject = TokenStream.Next()).getString("value").equals("to")) {
            throw new Exception("Premature \"to\" on line: " + TokenStream.GetCurrentLine());
        }
        line += ProcessForInitialization(TempObject, "to") + "; " + var + " <> ";

        // bir_sayi
        if ((TempObject = TokenStream.Next()).getString("value").equals("by")) {
            throw new Exception("Premature \"by\" on line: " + TokenStream.GetCurrentLine());
        }
        line += ProcessForInitialization(TempObject, "by") + "; " + var;

        // degisim_miktari
        TempObject = TokenStream.Next();
        boolean IsPlus;
        switch (TempObject.getString("value")) {
            case "+":
                IsPlus = true;
                line = line.replace("<>", "<") + " += (";
                break;
            case "-":
                IsPlus = false;
                line = line.replace("<>", ">") + " -= (";
                break;
            default:
                throw new Exception("Unknown operator on line: " + TokenStream.GetCurrentLine());
        }

        TempObject = TokenStream.Next();
        line += ProcessForInitialization(TempObject, "do") + ")) {";

        output.add(line);
        line = "";

        // Process body
        TempObject = TokenStream.Next();
        while (!(TempObject.getString("type").equals("kw")
                && TempObject.getString("value").equals("endfor"))) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
        }
        output.add("}");
        if (TempVariable) {
            SymbolTable.remove(var);
        }
        return output;
    }

    private String ProcessForInitialization(JSONObject TempObject, String CheckAgainst) throws Exception {
        String ValueString = "";
        while (!TempObject.getString("value").equals(CheckAgainst)) {
            switch (TempObject.getString("type")) {
                case "num":
                    if (TempObject.getString("subtype").equals("float")) {
                        throw new Exception("Floats are not allowed on line: " + TokenStream.GetCurrentLine());
                    }
                    ValueString += TempObject.getString("value") + " ";
                    break;
                case "op":
                    if (!"+-*/".contains(TempObject.getString("value"))) {
                        throw new Exception("Unknown operator on line: " + TokenStream.GetCurrentLine());
                    }
                    ValueString += TempObject.getString("value") + " ";
                    break;
                case "var":
                    if (TokenStream.Peek().getString("value").equals("[")) {
                        if (!SymbolTable.get(TempObject.getString("value")).equals("list")) {
                            throw new Exception("Not a list on line: " + TokenStream.GetCurrentLine());
                        }
                        if (!ArrayTable.get(TempObject.getString("value")).equals("int")) {
                            throw new Exception("Not an integer list on line: " + TokenStream.GetCurrentLine());
                        }
                        TokenStream.Next();
                        ValueString += TempObject.getString("value") + "[" + ProcessArrayIndice() + "] ";
                    } else {
                        if (!SymbolTable.containsKey(TempObject.getString("value"))
                                || !SymbolTable.get(TempObject.getString("value")).equals("int")) {
                            throw new Exception("Variable does not exist or is not integer on line: " + TokenStream.GetCurrentLine());
                        }
                        ValueString += TempObject.getString("value") + " ";
                    }
                    break;
                case "kw":
                    if (!FunctionTable.containsKey(TempObject.getString("value"))) {
                        throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
                    }
                    if (!FunctionTable.get(TempObject.getString("value")).contains("int")
                            && !(FunctionTable.get(TempObject.getString("value")).size() == 1
                            && FunctionTable.get(TempObject.getString("value")).get(0).equals(""))) {
                        throw new Exception("Function does not return integer on line: " + TokenStream.GetCurrentLine());
                    }
                    if (!TokenStream.Peek().getString("value").equals("(")) {
                        throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    TokenStream.Next(); // TODO 1
                    switch (TempObject.getString("value")) {
                        case "abs":
                            ValueString += "Math.abs" + ProcessParantheses() + " ";
                            break;
                        case "pow":
                            ValueString += "(int) Math.pow" + ProcessParantheses() + " ";
                            break;
                        case "sqrt":
                            ValueString += "(int) Math.sqrt" + ProcessParantheses() + " ";
                            break;
                        case "round":
                            ValueString += "Math.round" + ProcessParantheses() + " ";
                            break;
                        case "trunc":
                            ValueString += "(int) Math.ceil" + ProcessParantheses() + " ";
                            break;
                        default:
                            if (FunctionTable.get(TempObject.getString("value")).size() == 1
                                    && FunctionTable.get(TempObject.getString("value")).get(0).equals("")) {
                                ValueString += "(int) ";
                            }
                            ValueString += TempObject.getString("value") + ProcessParantheses() + " ";
                    }
                    break;
                default:
                    throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
            }
            TempObject = TokenStream.Next();
        }
        return ValueString.substring(0, ValueString.length() - 1);
    }

    private String ProcessParantheses() throws Exception {
        String output = "(";
        JSONObject TempObject = TokenStream.Next();
        if (TempObject.getString("type").equals("punc")
                && TempObject.getString("value").equals(")")) {
            return output + ")";
        }

        while (!(TempObject.getString("type").equals("punc")
                && TempObject.getString("value").equals(")"))) {
            switch (TempObject.getString("type")) {
                case "punc":
                    if (TempObject.getString("value").equals("{")
                            || TempObject.getString("value").equals("}")) {
                        throw new Exception("Unknown punctuation on line: " + TokenStream.GetCurrentLine());
                    }
                    if (TempObject.getString("value").equals("(")) {
                        output += ProcessParantheses() + " ";
                    } else {
                        output += TempObject.getString("value") + " ";
                    }
                    break;
                case "kw":
                    boolean ContainsParantheses = TokenStream.Peek().getString("value").equals("(");
                    switch (TempObject.getString("value")) {
                        case "mod":
                            output += " % ";
                            break;
                        case "not":
                            output += "!";
                            break;
                        case "and":
                            output += " && ";
                            break;
                        case "or":
                            output += " || ";
                            break;
                        case "abs":
                            if (!ContainsParantheses) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            output += "Math.abs" + ProcessParantheses() + " ";
                            break;
                        case "pow":
                            if (!ContainsParantheses) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            output += "Math.pow" + ProcessParantheses() + " ";
                            break;
                        case "sqrt":
                            if (!ContainsParantheses) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            output += "Math.sqrt" + ProcessParantheses() + " ";
                            break;
                        case "round":
                            if (!ContainsParantheses) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            output += "Math.round" + ProcessParantheses() + " ";
                            break;
                        case "trunc":
                            if (!ContainsParantheses) {
                                throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                            }
                            output += "Math.ceil" + ProcessParantheses() + " ";
                            break;
                        default:
                            if (FunctionTable.containsKey(TempObject.getString("value"))) {
                                if (!ContainsParantheses) {
                                    throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                                }
                                output += TempObject.getString("value") + ProcessParantheses() + " ";
                            } else {
                                throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
                            }
                    }
                    break;
                case "var":
                    if (!SymbolTable.containsKey(TempObject.getString("value"))) {
                        throw new Exception("Variable does not exist on line: " + TokenStream.GetCurrentLine());
                    }
                    if (TokenStream.Peek().getString("value").equals("[")) {
                        if (!SymbolTable.get(TempObject.getString("value")).equals("list")) {
                            throw new Exception("Not an array on line: " + TokenStream.GetCurrentLine());
                        }
                        String ArrayName = TempObject.getString("value");
                        TokenStream.Next();
                        String operand = ArrayName + "[" + ProcessArrayIndice() + "]";
                        output += operand + " ";
                    } else {
                        output += TempObject.getString("value") + " ";
                    }
                    break;
                case "num":
                    output += TempObject.getString("value") + " ";
                    break;
                case "str":
                    output += "\"" + TempObject.getString("value") + "\" ";
                    break;
                case "op":
                    output += TempObject.getString("value") + " ";
                    break;
                default:
                    throw new Exception("Unknown type on line: " + TokenStream.GetCurrentLine());
            }
            TempObject = TokenStream.Next();
        }

        return output.substring(0, output.length() - 1) + ")";
    }

    private List<String> ProcessKeywords(JSONObject InputKeyword) throws Exception {
        List<String> Output = new ArrayList<>();
        switch (InputKeyword.getString("value")) {
            case "if":
                Output.addAll(ProcessIf());
                break;
            case "while":
                Output.addAll(ProcessWhile());
                break;
            case "for":
                Output.addAll(ProcessFor());
                break;
            case "print":
                Output.add(ProcessPrint());
                break;
            case "return":
                Output.addAll(ProcessReturn());
                break;
            default:
                if (!FunctionTable.containsKey(InputKeyword.getString("value"))) {
                    throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
                }
                String line = InputKeyword.getString("value");
                InputKeyword = TokenStream.Next();
                if (!InputKeyword.getString("value").equals("(")) {
                    throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                }
                Output.add(line + ProcessParantheses()/* + ((TokenStream.Peek().getString("value").equals("eol")) ? ";" : "")*/);
        }
        return Output;
    }

    private List<String> ProcessReturn() throws Exception {
        JSONObject TempObject = TokenStream.Next();
        if (FunctionReturnType.equals("none")) {
            if (!TempObject.getString("value").equals("eol")) {
                throw new Exception("Returning something from a \"none\" function on line: " + TokenStream.GetCurrentLine());
            }
            return Arrays.asList("return;");
        }

        List<String> ParameterList = new ArrayList<>();
        String line = "return ";

        if (TempObject.getString("value").equals("eol")) {
            throw new Exception("Premature end of line on line: " + TokenStream.GetCurrentLine());
        }
        while (!TempObject.getString("value").equals("eol")) {
            switch (TempObject.getString("type")) {
                case "num":
                    if (FunctionReturnType.equals("float")
                            && TempObject.getString("value").contains(".")) {
                        ParameterList.add("float");
                    } else if (FunctionReturnType.equals("int")
                            && !TempObject.getString("value").contains(".")) {
                        ParameterList.add("int");
                    } else if (FunctionReturnType.equals("str")
                            && !ParameterList.contains("str")
                            && TokenStream.Peek().getString("value").equals("eol")) {
                        throw new Exception("Returning only numbers from a string returning function on line: " + TokenStream.GetCurrentLine());
                    } else if (!FunctionReturnType.equals("str")) {
                        throw new Exception("Incompatible return types on line: " + TokenStream.GetCurrentLine());
                    }
                    line += TempObject.getString("value") + " ";
                    break;
                case "var":
                    if (FunctionReturnType.equals("int")
                            || FunctionReturnType.equals("float")) {
                        if (!SymbolTable.get(TempObject.getString("value")).equals(FunctionReturnType)) {
                            throw new Exception("Incompatible return types on line: " + TokenStream.GetCurrentLine());
                        }
                    }
                    ParameterList.add(SymbolTable.get(TempObject.getString("value")));
                    line += TempObject.getString("value") + " ";
                    break;
                case "kw":
                    if (!FunctionTable.containsKey(TempObject.getString("value"))) {
                        throw new Exception("Unknown keyword on line: " + TokenStream.GetCurrentLine());
                    }
                    if (!FunctionReturnType.equals("str")) {
                        if (!FunctionTable.get(TempObject.getString("value")).contains(FunctionReturnType)
                                && !FunctionTable.get(TempObject.getString("value")).contains("str")) {
                            line += "(" + FunctionReturnType + ") ";
                        } else if (FunctionTable.get(TempObject.getString("value")).contains("str")) {
                            throw new Exception("Incompatible return types on line: " + TokenStream.GetCurrentLine());
                        }
                    }
                    ParameterList.add(FunctionReturnType);
                    line += TempObject.getString("value");

                    TempObject = TokenStream.Next();
                    if (TempObject.getString("value").equals("(")) {
                        line += ProcessParantheses() + " ";
                    } else {
                        throw new Exception("Missing paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    break;
                case "str":
                    if (!FunctionReturnType.equals("str")) {
                        throw new Exception("Incompatible return types on line: " + TokenStream.GetCurrentLine());
                    }
                    ParameterList.add(FunctionReturnType);
                    line += "\"" + TempObject.getString("value") + "\" ";
                    break;
                case "punc":
                    // return'de parantez icinde olan seyleri denetlemiyoruz,
                    // parantezde olan parantezde kalir
                    if (TempObject.getString("value").equals("(")) {
                        line += ProcessParantheses() + " ";
                    } else {
                        throw new Exception("Mising paranthesis on line: " + TokenStream.GetCurrentLine());
                    }
                    ParameterList.add("()");
                    break;
                case "op":
                    if (TokenStream.Peek().getString("value").equals("eol")) {
                        throw new Exception("Premature end of line on line: " + TokenStream.GetCurrentLine());
                    }
                    switch (FunctionReturnType) {
                        case "int":
                        case "float":
                            if (!"+-*/".contains(TempObject.getString("value"))) {
                                throw new Exception("Unknown operator on line: " + TokenStream.GetCurrentLine());
                            }
                            line += TempObject.getString("value") + " ";
                            ParameterList.add(TempObject.getString("value"));
                            break;
                        case "str":
                            if (!TempObject.getString("value").equals("+")) {
                                throw new Exception("Usage of some operator other than + on strings on line: " + TokenStream.GetCurrentLine());
                            }
                            line += "+ ";
                            ParameterList.add("+");
                            break;
                    }
                    break;
            }
            TempObject = TokenStream.Next();
        }
        FunctionReturnSatisfied = true;
        return Arrays.asList(line.substring(0, line.length() - 1) + ";");
    }
}
