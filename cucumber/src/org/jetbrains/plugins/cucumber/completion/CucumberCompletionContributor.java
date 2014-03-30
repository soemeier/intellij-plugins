package org.jetbrains.plugins.cucumber.completion;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.TemplateBuilder;
import com.intellij.codeInsight.template.TemplateBuilderFactory;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.*;
import org.jetbrains.plugins.cucumber.psi.impl.GherkinExamplesBlockImpl;
import org.jetbrains.plugins.cucumber.psi.impl.GherkinScenarioOutlineImpl;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;
import org.jetbrains.plugins.cucumber.steps.CucumberStepsIndex;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author yole
 */
public class CucumberCompletionContributor extends CompletionContributor {
  private static final Map<String, String> GROUP_TYPE_MAP = new HashMap<String, String>();
  private static final Map<String, String> INTERPOLATION_PARAMETERS_MAP = new HashMap<String, String>();
  static {
    GROUP_TYPE_MAP.put("(.*)", "<string>");
    GROUP_TYPE_MAP.put("(.+)", "<string>");
    GROUP_TYPE_MAP.put("([^\"]*)", "<string>");
    GROUP_TYPE_MAP.put("([^\"]+)", "<string>");
    GROUP_TYPE_MAP.put("(\\d*)", "<number>");
    GROUP_TYPE_MAP.put("(\\d+)", "<number>");
    INTERPOLATION_PARAMETERS_MAP.put("#\\{[^\\}]*\\}", "<param>");
  }

  private static final int SCENARIO_KEYWORD_PRIORITY = 70;
  private static final int SCENARIO_OUTLINE_KEYWORD_PRIORITY = 60;
  public static final Pattern POSSIBLE_GROUP_PATTERN = Pattern.compile("\\(([^\\)]*)\\)");
  public static final Pattern QUESTION_MARK_PATTERN = Pattern.compile("([^\\\\])\\?:?");
  public static final Pattern PARAMETERS_PATTERN = Pattern.compile("<string>|<number>|<param>|" + POSSIBLE_GROUP_PATTERN);
  public static final String INTELLIJ_IDEA_RULEZZZ = "IntellijIdeaRulezzz";
  public static final String REPLACED_OPEN_BRACKET = "&%@";
  public static final String REPLACED_CLOSE_BRACKET = "@&%";
  public static final Pattern REPLACED_OPEN_BRACKET_PATTERN = Pattern.compile("(" + REPLACED_OPEN_BRACKET + ")");
  public static final Pattern REPLACED_CLOSED_BRACKET_PATTERN = Pattern.compile("(" + REPLACED_CLOSE_BRACKET + ")");

  public CucumberCompletionContributor() {
    final PsiElementPattern.Capture<PsiElement> inScenario = psiElement().inside(psiElement().withElementType(GherkinElementTypes.SCENARIOS));
    final PsiElementPattern.Capture<PsiElement> inStep = psiElement().inside(psiElement().withElementType(GherkinElementTypes.STEP));

    extend(CompletionType.BASIC, psiElement().inFile(psiElement(GherkinFile.class)), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        final PsiFile psiFile = parameters.getOriginalFile();
        if (psiFile instanceof GherkinFile) {
          final PsiElement position = parameters.getPosition();

          // if element isn't under feature declaration - suggest feature in autocompletion
          // but don't suggest scenario keywords inside steps
          final PsiElement coveringElement = PsiTreeUtil.getParentOfType(position, GherkinStep.class, GherkinFeature.class, PsiFileSystemItem.class);
          if (coveringElement instanceof PsiFileSystemItem) {
            addFeatureKeywords(result, psiFile);
          } else if (coveringElement instanceof GherkinFeature) {
            addScenarioKeywords(result, psiFile, position);
          }
        }
      }
    });

    extend(CompletionType.BASIC, inScenario.andNot(inStep), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        addStepKeywords(result, parameters.getOriginalFile());
      }
    });

    extend(CompletionType.BASIC, inStep, new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        addStepDefinitions(result, parameters.getOriginalFile());
      }
    });
  }

  private static void addScenarioKeywords(CompletionResultSet result, PsiFile originalFile, PsiElement originalPosition) {
    final Project project = originalFile.getProject();
    final GherkinKeywordTable table = GherkinKeywordTable.getKeywordsTable(originalFile, project);
    final List<String> keywords = new ArrayList<String>();

    if (!haveBackground(originalFile)) {
      keywords.addAll(table.getBackgroundKeywords());
    }

    final PsiElement prevElement = getPreviousElement(originalPosition);
    if (prevElement != null && prevElement.getNode().getElementType() == GherkinTokenTypes.SCENARIO_KEYWORD) {
      String scenarioKeyword = (String)table.getScenarioKeywords().toArray()[0];
      result = result.withPrefixMatcher(result.getPrefixMatcher().cloneWithPrefix(scenarioKeyword + " " + result.getPrefixMatcher().getPrefix()));

      boolean haveColon = false;
      final String elementText = originalPosition.getText();
      final int rulezzIndex = elementText.indexOf(INTELLIJ_IDEA_RULEZZZ);
      if (rulezzIndex >= 0) {
        haveColon = elementText.substring(rulezzIndex + INTELLIJ_IDEA_RULEZZZ.length()).trim().startsWith(":");
      }

      addKeywordsToResult(table.getScenarioOutlineKeywords(), result, !haveColon, SCENARIO_OUTLINE_KEYWORD_PRIORITY, !haveColon);
    } else {
      addKeywordsToResult(table.getScenarioKeywords(), result, true, SCENARIO_KEYWORD_PRIORITY, true);
      addKeywordsToResult(table.getScenarioOutlineKeywords(), result, true, SCENARIO_OUTLINE_KEYWORD_PRIORITY, true);
    }

    if (PsiTreeUtil.getParentOfType(originalPosition, GherkinScenarioOutlineImpl.class, GherkinExamplesBlockImpl.class) != null) {
      keywords.addAll(table.getExampleSectionKeywords());
    }
    // add to result
    addKeywordsToResult(keywords, result, true);
  }

  private static PsiElement getPreviousElement(PsiElement element) {
    PsiElement prevElement = element.getPrevSibling();
    if (prevElement != null && prevElement instanceof PsiWhiteSpace) {
      prevElement = prevElement.getPrevSibling();
    }
    return prevElement;
  }

  private static void addFeatureKeywords(CompletionResultSet result, PsiFile originalFile) {
    final Project project = originalFile.getProject();
    final GherkinKeywordTable table = GherkinKeywordTable.getKeywordsTable(originalFile, project);

    final Collection<String> keywords = table.getFeaturesSectionKeywords();
    // add to result
    addKeywordsToResult(keywords, result, true);
  }

  private static void addKeywordsToResult(final Collection<String> keywords,
                                          final CompletionResultSet result,
                                          final boolean withColonSuffix) {
    addKeywordsToResult(keywords, result, withColonSuffix, 0, true);
  }

  private static void addKeywordsToResult(final Collection<String> keywords,
                                          final CompletionResultSet result,
                                          final boolean withColonSuffix, int priority, boolean withSpace) {
    for (String keyword : keywords) {
      LookupElement element = createKeywordLookupElement(withColonSuffix ? keyword + ":" : keyword, withSpace);

      result.addElement(PrioritizedLookupElement.withPriority(element, priority));
    }
  }

  private static LookupElement createKeywordLookupElement(final String keyword, boolean withSpace) {
    LookupElement result = LookupElementBuilder.create(keyword);
    if (withSpace) {
      result = TailTypeDecorator.withTail(result, TailType.SPACE);
    }

    return result;
  }

  private static boolean haveBackground(PsiFile originalFile) {
    PsiElement scenarioParent = PsiTreeUtil.getChildOfType(originalFile, GherkinFeature.class);
    if (scenarioParent == null) {
      scenarioParent = originalFile;
    }
    final GherkinScenario[] scenarios = PsiTreeUtil.getChildrenOfType(scenarioParent, GherkinScenario.class);
    if (scenarios != null) {
      for (GherkinScenario scenario : scenarios) {
        if (scenario.isBackground()) {
          return true;
        }
      }
    }
    return false;
  }

  private static void addStepKeywords(CompletionResultSet result, PsiFile file) {
    if (!(file instanceof GherkinFile)) return;
    final GherkinFile gherkinFile = (GherkinFile)file;

    addKeywordsToResult(gherkinFile.getStepKeywords(), result, false);
  }

  private static void addStepDefinitions(CompletionResultSet result, PsiFile file) {
    result = result.withPrefixMatcher(new PlainPrefixMatcher(result.getPrefixMatcher().getPrefix()));
    final List<AbstractStepDefinition> definitions = CucumberStepsIndex.getInstance(file.getProject()).getAllStepDefinitions(file);
    for (AbstractStepDefinition definition : definitions) {
      String text = definition.getCucumberRegex();
      if (text != null) {
        // trim regexp line start/end markers
        if (text.startsWith("^")) {
          text = text.substring(1);
        }
        if (text.endsWith("$")) {
          text = text.substring(0, text.length() - 1);
        }
        text = StringUtil.replace(text, "\\\"", "\"");
        //Escaped brackets must not be replaced by later process. Therefore it is taken
        text = StringUtil.replace(text, "\\(", REPLACED_OPEN_BRACKET);
        text = StringUtil.replace(text, "\\)", REPLACED_CLOSE_BRACKET);



        for (Map.Entry<String, String> group : GROUP_TYPE_MAP.entrySet()) {
          text = StringUtil.replace(text, group.getKey(), group.getValue());
        }

        for (Map.Entry<String, String> group : INTERPOLATION_PARAMETERS_MAP.entrySet()) {
          text = text.replaceAll(group.getKey(), group.getValue());
        }

        final List<RangeWithPossibleValues> ranges = new ArrayList<RangeWithPossibleValues>();
        Matcher m = QUESTION_MARK_PATTERN.matcher(text);
        if (m.find()) {
          text = m.replaceAll("$1");
        }

        m = PARAMETERS_PATTERN.matcher(text);
        int parameterIndex = 0;
        int offset = 0;
        while (m.find()) {
          if (m.group().startsWith("(")) {
            ranges.add(new RangeWithPossibleValues(new TextRange(m.start() - offset, m.end() -offset -2),
                                                   determinePossibleVariableTypes(parameterIndex, definition)));
            offset += 2;
          } else {
            ranges.add(new RangeWithPossibleValues(new TextRange(m.start() - offset, m.end() - offset),
                                                   determinePossibleVariableTypes(parameterIndex, definition)));
          }
          parameterIndex++;
        }
        text = StringUtil.replace(text,"(", "");
        text = StringUtil.replace(text, ")", "");

        m = REPLACED_OPEN_BRACKET_PATTERN.matcher(text);

        text = normalizeReplacedBracket(ranges, m, "(");

        m = REPLACED_CLOSED_BRACKET_PATTERN.matcher(text);

        text = normalizeReplacedBracket(ranges, m, ")");

        final PsiElement element = definition.getElement();
        final LookupElementBuilder lookup = element != null 
                                            ? LookupElementBuilder.create(element, text).bold()
                                            : LookupElementBuilder.create(text);

        result.addElement(lookup.withInsertHandler(new StepInsertHandler(ranges)));
      }
    }
  }

  private static String normalizeReplacedBracket(List<RangeWithPossibleValues> ranges, Matcher m, String bracket) {
    String text;
    while (m.find()) {
      for (RangeWithPossibleValues range : ranges) {
        if (range.getRange().getStartOffset() > m.start()) {
          range.setRange(new TextRange(range.getRange().getStartOffset() -2, range.getRange().getEndOffset() -2));
        } else if (range.getRange().getEndOffset() > m.start()) {
          range.setRange(new TextRange(range.getRange().getStartOffset(), range.getRange().getEndOffset() -2));
        }

      }
    }
    text = m.replaceAll(bracket);
    return text;
  }


  @Nullable
  private static List<String> determinePossibleVariableTypes(int parameterIndex, AbstractStepDefinition definition) {
    if (definition.getVariableNames().size()<= parameterIndex) {
      return null;
    }
    String parameterName = definition.getVariableNames().get(parameterIndex);
    return definition.getPossibleVariableTypes(parameterName).getValidValues();

  }

  private static class StepInsertHandler implements InsertHandler<LookupElement> {
    private final List<RangeWithPossibleValues> ranges;

    private StepInsertHandler(List<RangeWithPossibleValues> ranges) {
      this.ranges = ranges;
    }

    @Override
    public void handleInsert(final InsertionContext context, LookupElement item) {
      if (!ranges.isEmpty()) {
        final PsiElement element = context.getFile().findElementAt(context.getStartOffset());
        final GherkinStep step = PsiTreeUtil.getParentOfType(element, GherkinStep.class);
        if (step != null) {
          final TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(step);
          int off = context.getStartOffset() - step.getTextRange().getStartOffset();
          final String stepText = step.getText();
          for (RangeWithPossibleValues groupRangeWithPossibleValue : ranges) {
            final TextRange shiftedRange = groupRangeWithPossibleValue.getRange().shiftRight(off);
            final String matchedText = shiftedRange.substring(stepText);

            builder.replaceRange(shiftedRange.shiftRight(step.getTextRange().getStartOffset()),
                                 createExpression(matchedText, groupRangeWithPossibleValue.getPossibleValueForRange()));
          }
          builder.run(context.getEditor(), false);
        }
      }
    }

    private static ConstantNode createExpression(String matchedText, @Nullable final List<String> possibleValueForRange) {
      return new ConstantNode(matchedText) {
        @Override
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
          if (possibleValueForRange != null) {
            LookupElement[] lookupElements = new LookupElement[possibleValueForRange.size()];
            for (int i = 0; i < possibleValueForRange.size(); i++) {
              lookupElements[i] = LookupItem.fromString(possibleValueForRange.get(i));
            }
            return lookupElements;
          }
          return new LookupElement[]{};
        }
      };
    }
  }
}
