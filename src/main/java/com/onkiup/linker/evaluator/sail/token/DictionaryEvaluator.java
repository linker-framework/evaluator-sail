package com.onkiup.linker.evaluator.sail.token;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.LisaDictionaryToken;
import com.onkiup.linker.grammar.sail.token.LisaValueToken;
import com.onkiup.linker.parser.annotation.AdjustPriority;
import com.onkiup.linker.parser.annotation.IgnoreCharacters;

@AdjustPriority(1000) // makes sure that LisaListToken has priority over this token
@IgnoreCharacters(" \r\n\t")
public class DictionaryEvaluator<X> implements SailEvaluator<LisaDictionaryToken, Map<CharSequence,X>> {

  @Override
  public Map<CharSequence,X> evaluate() {
    LisaDictionaryToken base = base();
    LisaDictionaryToken.Member[] members = base.members();
    return Arrays.stream(members)
        .collect(Collectors.toMap(LisaDictionaryToken.Member::name, member -> {
          LisaValueToken token = member.token();
          if (token == null) {
            return null;
          }
          return (X) token.as(Evaluator.class).value();
        }));
  }
}

