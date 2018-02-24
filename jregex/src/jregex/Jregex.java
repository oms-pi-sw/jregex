/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jregex;

import ansiTTY.ansi.Ansi;
import static ansiTTY.ansi.Ansi.print;
import static ansiTTY.ansi.Ansi.println;
import ansiTTY.ansi.format.AnsiAttribute;
import ansiTTY.ansi.format.AnsiColor;
import loggermanager.LoggerManager;
import static loggermanager.LoggerManager.log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Niccolò Ferrari
 */
public class Jregex {

  //Options
  private static final String REGEX = "pattern";
  private static final String HELP = "help";
  private static final String VERSION = "version";
  private static final String FILENAME = "filename";
  private static final String MATCH = "match";
  private static final String ALL = "all";
  private static final String ICASE = "icase";
  private static final String COLOR = "color";
  private static final String LINE = "line-number";

  //Version
  private static final int MAJOR = 0;
  private static final int MINOR = 1;
  private static final int REVISION = 0;

  private static String version() {
    return MAJOR + "." + MINOR + "." + REVISION;
  }

  private static Options setOptions() {
    Options options = new Options();
    OptionGroup required = new OptionGroup();

    //OptionGroup of mutual exclusive required options
    Option regex = new Option("e", REGEX, true, "The regex mattern to match.");
    regex.setArgName(REGEX);
    regex.setRequired(true);
    regex.setOptionalArg(false);
    regex.setType(String.class);
    required.addOption(regex);

    Option help = new Option("h", HELP, false, "Print help.");
    help.setArgName(HELP);
    help.setRequired(true);
    required.addOption(help);

    Option version = new Option("v", VERSION, false, "Print version.");
    version.setArgName(VERSION);
    version.setRequired(true);
    required.addOption(version);

    //Other options
    Option filename = new Option("f", FILENAME, true, "The input filename.");
    filename.setArgName(FILENAME);
    filename.setOptionalArg(false);
    filename.setRequired(false);
    filename.setType(String.class);
    options.addOption(filename);

    Option match = new Option("m", MATCH, true, "Print only matching lines. If option \"all\" is enabled, prints only full-matching lines.");
    match.setArgName(MATCH);
    match.setRequired(false);
    match.setOptionalArg(true);
    match.setType(String.class);
    options.addOption(match);

    Option all = new Option("a", ALL, false, "Read all file or stdin until EOF and then match regex. Be careful with big text files.");
    all.setArgName(ALL);
    all.setRequired(false);
    options.addOption(all);

    Option icase = new Option("i", ICASE, false, "Case insensitive flag.");
    icase.setArgName(ICASE);
    icase.setRequired(false);
    options.addOption(icase);

    Option color = new Option("c", COLOR, true, "If no color is specified disable text coloring. You can specify one of these colors:"
            + System.lineSeparator() + "* black"
            + System.lineSeparator() + "* red"
            + System.lineSeparator() + "* green"
            + System.lineSeparator() + "* yellow"
            + System.lineSeparator() + "* blue"
            + System.lineSeparator() + "* magenta"
            + System.lineSeparator() + "* cyan"
            + System.lineSeparator() + "* white");
    color.setArgName(COLOR);
    color.setRequired(false);
    color.setOptionalArg(true);
    color.setType(String.class);
    options.addOption(color);

    Option line = new Option("l", LINE, false, "Disable line number.");
    line.setArgName(LINE);
    line.setRequired(false);
    options.addOption(line);

    required.setRequired(true);
    options.addOptionGroup(required);

    return options;
  }

  public static enum PrintPolicy {
    ANY, MATCH, FULL_MATCH;
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    try {
      Options options = setOptions();
      DefaultParser parser = new DefaultParser();
      CommandLine commandLine = parser.parse(options, args);

      if (commandLine.hasOption(HELP)) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jregex", options);
      } else if (commandLine.hasOption(VERSION)) {
        System.out.println("jregex: v" + version());
      } else if (commandLine.hasOption(REGEX)) {
        //PATTERN
        final String pattern = commandLine.getOptionValue(REGEX);

        //INPUT
        String filename = ((commandLine.hasOption(FILENAME) && commandLine.getOptionValue(FILENAME) != null) ? commandLine.getOptionValue(FILENAME) : null);
        BufferedReader in = new BufferedReader(filename != null ? new FileReader(new File(filename)) : new InputStreamReader(System.in));

        //COLOR
        AnsiColor color = AnsiColor.GREEN;
        if (commandLine.hasOption(COLOR)) {
          String _color = commandLine.getOptionValue(COLOR);
          if (_color == null) {
            color = null;
          } else {
            ObservableList<AnsiColor> colorList = FXCollections.observableArrayList(FXCollections.observableArrayList(AnsiColor.values()).stream().filter(_c -> _c.toString().equalsIgnoreCase(_color)).collect(Collectors.toList()));
            if (colorList != null && colorList.size() == 1) {
              color = colorList.get(0);
            }
          }
        }

        //ICASE
        boolean icase = commandLine.hasOption(ICASE);

        //LINE NUMBER
        boolean line = !commandLine.hasOption(LINE);

        //MATCH
        PrintPolicy policy = PrintPolicy.ANY;
        if (commandLine.hasOption(MATCH)) {
          policy = PrintPolicy.MATCH;
        }
        String opt_match;
        if ((opt_match = commandLine.getOptionValue(MATCH)) != null && opt_match.equalsIgnoreCase("all")) {
          policy = PrintPolicy.FULL_MATCH;
        }

        //ALL
        boolean all = commandLine.hasOption(ALL);

        Jregex jr = new Jregex(pattern, in, all, color, icase, line, policy);
        jr.start();
      }
    } catch (ParseException ex) {
      log(LoggerManager.FATAL, "OPTION ERROR: " + ex.getMessage());
    } catch (FileNotFoundException ex) {
      log(LoggerManager.FATAL, "INPUT FILE ERROR: " + ex.getMessage());
    } catch (IOException ex) {
      log(LoggerManager.FATAL, "IO ERROR: " + ex.getMessage());
    } catch (Exception ex) {
      log(LoggerManager.FATAL, ex, ex);
    }
  }

  private final Pattern pattern;
  private final BufferedReader in;
  private final boolean all;
  private final AnsiColor color;
  private final boolean icase;
  private final boolean line;
  private final PrintPolicy policy;

  public Jregex(String pattern, BufferedReader in, boolean all, AnsiColor color, boolean icase, boolean line, PrintPolicy policy) {
    this.pattern = (!icase ? Pattern.compile(pattern) : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
    this.in = in;
    this.all = all;
    this.color = color;
    this.icase = icase;
    this.line = line;
    this.policy = policy;
  }

  public void start() throws IOException {
    String _line;
    long lnum = 0;
    boolean match = true;
    StringBuilder _file = new StringBuilder("");
    String r = Ansi.ansi()
            .format().attribute(((color != null) ? AnsiAttribute.INTENSITY_BOLD : null))
            .format().fg(color)
            .a("$0")
            .format().reset().toString();

    while ((_line = in.readLine()) != null) {
      lnum++;
      if (all) {
        if (lnum > 1) {
          _file.append(System.lineSeparator());
        }
        _file.append(_line);
      } else {
        Matcher m = pattern.matcher(_line);

        boolean _match = m.find();
        if (policy.equals(PrintPolicy.MATCH)) {
          match = _match;
        } else if (policy.equals(PrintPolicy.FULL_MATCH)) {
          match = m.matches();
        }

        if (match) {
          String res = m.replaceAll(r);
          if (line) {
            print(Ansi.ansi()
                    .format().reset()
                    .format().attribute(((color != null && _match) ? AnsiAttribute.INTENSITY_BOLD : null))
                    .format().fg(((color != null && _match) ? AnsiColor.YELLOW : null))
                    .a("line #").a(lnum).a(":")
                    .format().reset()
                    .a("\t"));
          }
          println(res);
        }
      }
    }
    if (all) {
      Matcher m = pattern.matcher(_file.toString());
      String res = m.replaceAll(r);
      println(res);
    }
  }

  public Pattern getPattern() {
    return pattern;
  }

  public BufferedReader getIn() {
    return in;
  }

  public boolean isAll() {
    return all;
  }

  public AnsiColor getColor() {
    return color;
  }

  public boolean isIcase() {
    return icase;
  }

  public boolean isLine() {
    return line;
  }

  public PrintPolicy getPolicy() {
    return policy;
  }
}
