package com.kamron.pogoiv.widgets;

import android.content.Context;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.kamron.pogoiv.R;
import com.kamron.pogoiv.scanlogic.Pokemon;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Spinner formatter.
 */
public class PokemonSpinnerAdapter extends ArrayAdapter<Pokemon> {
    private final Context context;
    private final int spinnerLayoutXml;
    private ArrayList<Pokemon> pokemons;

    public PokemonSpinnerAdapter(Context context, int spinnerLayoutXml, ArrayList<Pokemon> pokemons) {
        super(context, spinnerLayoutXml, pokemons);
        this.context = context;
        this.spinnerLayoutXml = spinnerLayoutXml;
        this.pokemons = pokemons;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, parent);
    }

    /**
     * Updates the spinner with new information.
     *
     * @param list the new list of pokemon to show in the spinner
     */
    public void updatePokemonList(ArrayList<Pokemon> list) {
        pokemons = sortByForms(list);
        clear();
        addAll(pokemons);
        notifyDataSetChanged();

    }

    private static class GeneralFormNameComparator implements Comparator<Pokemon> {

        public static final GeneralFormNameComparator INSTANCE = new GeneralFormNameComparator();

        @Override
        public int compare(Pokemon p1, Pokemon p2) {
            // First: Normal forms appear first, Boolean.compare() sorts "false" before "true": invert it directly.
            int sort = Boolean.compare(p2.isNormalForm, p1.isNormalForm);
            if (sort != 0) {
                return sort;
            }
            // Then sort alphabetically the form names
            return p1.formName.compareTo(p2.formName);
        }
    }

    @AllArgsConstructor
    private static class SpecificFormNameComparator extends GeneralFormNameComparator {

        private final String internalFormName;

        public static void sort(ArrayList<Pokemon> list) {
            if (!list.isEmpty()) {
                Collections.sort(list, new SpecificFormNameComparator(list.get(0).internalFormName));
            }
        }

        @Override
        public int compare(Pokemon p1, Pokemon p2) {
            boolean match1 = p1.internalFormName.equals(internalFormName);
            boolean match2 = p2.internalFormName.equals(internalFormName);
            int sort = Boolean.compare(match2, match1);
            if (sort == 0) {
                return super.compare(p1, p2);
            }
            // Only if all of them are indifferent, sort them using the pokedex number.
            return p1.number - p2.number;
        }
    }

    private static void addEvolutions(Pokemon pokemon, ArrayList<Pokemon> source, ArrayList<Pokemon> list) {
        list.add(pokemon);
        for (Pokemon evolution : pokemon.evolutions) {
            int index = source.indexOf(evolution);
            if (index >= 0) {
                addEvolutions(source.remove(index), source, list);
            }
        }
    }

    /**
     * Sorts the given list to group them by the forms, listing the normal form first and then alphabetically the rest.
     * Each group is then sorted by their "evolution depth" and lastly the pokedex number. Some evolutions have a lower
     * pokedex number (Munchlax (446) and Snorlax (143)) while others have multiple numbers for the same evolution depth
     * (the Eeveelutions).
     *
     * @param list The list which needs to be sorted.
     * @return A copy of the list sorted according to the mentioned rules.
     */
    private ArrayList<Pokemon> sortByForms(ArrayList<Pokemon> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        // Create a map for all forms, which we are going to assign the pokemons to
        Map<String, ArrayList<Pokemon>> formNames = new HashMap<>();
        // Also store a sample pokemon for each form, to have an order of the forms before filling them
        ArrayList<Pokemon> formSamples = new ArrayList<>();
        for (Pokemon pokemon : list) {
            if (!formNames.containsKey(pokemon.internalFormName)) {
                formNames.put(pokemon.internalFormName, new ArrayList<Pokemon>());
                formSamples.add(pokemon);
            }
        }
        Collections.sort(formSamples, GeneralFormNameComparator.INSTANCE);
        final ArrayList<String> formOrder = new ArrayList<>(formSamples.size());
        for (Pokemon y : formSamples) {
            formOrder.add(y.internalFormName);
        }
        // Now sort the pokemons by the evolution depth, this means we can get each pokemon and add its evolutions and
        // the first will be the lowest of that form.
        ArrayList<Pokemon> depthSorted = new ArrayList<>(list);
        Collections.sort(depthSorted, new Comparator<Pokemon>() {
            @Override
            public int compare(Pokemon p1, Pokemon p2) {
                int sort = Integer.compare(p1.base.getEvolutionDepth(), p2.base.getEvolutionDepth());
                if (sort != 0) {
                    return sort;
                }
                return Integer.compare(formOrder.indexOf(p1.internalFormName), formOrder.indexOf(p2.internalFormName));
            }
        });
        // Now go through the list until it's empty, remove the first element and add it's evolutions to the given form.
        // When it goes through the evolutions, it is also removing those from the list. So (with the sorting from) the
        // first element is the lowest evolution, of that branch.
        while (!depthSorted.isEmpty()) {
            Pokemon pokemon = depthSorted.remove(0);
            addEvolutions(pokemon, depthSorted, formNames.get(pokemon.internalFormName));
        }
        // Now reconstruct the original list, by going through each "root" and adding the complete list associated to
        // it. Of course we need to sort them too, if it has multiple form names.
        ArrayList<Pokemon> returnerList = new ArrayList<>(list.size());
        for (String formName : formOrder) {
            ArrayList<Pokemon> evolved = formNames.get(formName);
            SpecificFormNameComparator.sort(evolved);
            returnerList.addAll(evolved);
        }
        return returnerList;
    }


    /**
     * Gets the view of the single element when not in dropdown mode
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        /*
        ConstraintLayout row = (ConstraintLayout) inflater.inflate(spinnerLayoutXml, parent, false);
        //TextView row = (TextView) inflater.inflate(spinnerLayoutXml, parent, false);
        Pokemon pokemon = pokemons.get(position);

        ((TextView) row.getViewById(R.id.spinnerPokedexNum)).setText("");
        ((TextView) row.getViewById(R.id.spinnerPokeName)).setText(pokemon.base.displayName);
        ((TextView) row.getViewById(R.id.spinnerForm)).setText("▼");
        */

        TextView row = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);

        Pokemon pokemon = pokemons.get(position);
        row.setText(pokemon.toString() + "  ▼");

        return row;
    }


    /**
     * Gets the view of the drop-down elements
     * @param position
     * @param parent
     * @return
     */
    private View getCustomView(int position, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Pokemon pokemon = pokemons.get(position);
        ConstraintLayout row = (ConstraintLayout) inflater.inflate(spinnerLayoutXml, parent, false);
        ((TextView) row.getViewById(R.id.spinnerPokedexNum)).setText("#" + pokemon.number);
        ((TextView) row.getViewById(R.id.spinnerPokeName)).setText(pokemon.base.displayName);
        ((TextView) row.getViewById(R.id.spinnerForm)).setText(shortenFormName(pokemon.formName));
        ((TextView) row.getViewById(R.id.spinnerForm)).setTextColor(getFormColor(pokemon.formName));
        /*
        TextView row = (TextView) inflater.inflate(spinnerLayoutXml, parent, false);
        Pokemon pokemon = pokemons.get(position);
        String text = String.format("#%d %s", pokemon.number + 1, pokemon.toString());

        int padding = GuiUtil.dpToPixels(5, context);
        row.setPadding(padding, 0, 0, padding);
        row.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        row.setGravity(View.TEXT_ALIGNMENT_TEXT_START);
        row.setText(text);
        */

        return row;
    }

    private String shortenFormName(String formName) {
        if (formName.length() < 5){
            return formName;
        }
        return formName.substring(0, formName.length()-5);

    }

    private int getFormColor(String formName) {
        String shortString = "";
        if (formName.length() >= 3) {
            shortString = formName.substring(0, 2);
        }

        int colRaw = shortString.hashCode();
        int rndDecIndex = colRaw % 3;

        ArrayList<Integer> colList = new ArrayList(3);
        colList.add(new Integer(Color.red(colRaw)));
        colList.add(new Integer(Color.green(colRaw)));
        colList.add(new Integer(Color.blue(colRaw)));

        if (colList.get(rndDecIndex) > 150) {
            colList.set(rndDecIndex, new Integer(50));
        }
        if (colList.get((rndDecIndex + 1) % 3) < 150) {
            colList.set((rndDecIndex + 1) % 3, new Integer(150));
        }

        return Color.rgb(colList.get(0), colList.get(1), colList.get(2));
    }
}
