package tap.sample;

// This example shows how a HashMap is passed into mappers using Phase.mapParam()

import java.util.StringTokenizer;
import tap.sample.CountRec;          
import tap.*;

public class PassHashMap {
    
    public static void main(String[] args) throws Exception {
        CommandOptions o = new CommandOptions(args);
        Tap tap = new Tap(o);
        HashMap<String,Integer> scores = new HashMap<String,Integer>();
        scores.put("apple", 2);
        scores.put("peach", -1);
        scores.put("orange", 5);
        createPhase()
            .reads(o.input).map(ScoreMapper.class).mapParam("scoreParam", scores)
            .groupBy("word")
            .writes(o.output).reduce(ScoreReducer.class);
        return make();
    }
    
    public static class ScoreMapper extends TapMapper {
        HashMap<String, Integer> scoreParam;
        @Override
        public void map(String in, Pipe<CountRec> out) {
            StringTokenizer tokenizer = new StringTokenizer(in);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                Integer score = scoreParam.get(token.toLowerCase());
                if (score == null) score = 1;
                out.put(CountRec.newBuilder().setWord(token).setCount(score).build()));
            }
        }
    }
    
    public static class ScoreReducer extends TapReducer {
        @Override
        public void reduce(Pipe<CountRec> in, Pipe<CountRec> out) {
            String word = null;
            int count = 0;
            for (CountRec rec : in) {
                if (word == null) word = rec.getWord();
                count += rec.getCount();
            }
            out.put(CountRec.newBuilder().setWord(word).setCount(count).build());
        }
    }
    
    /*
     * CountRec.proto
     *
     
     package tap.sample;
     
     message CountRec {
     required string word = 1;
     required int32 count = 2;
     }
     
     *
     */