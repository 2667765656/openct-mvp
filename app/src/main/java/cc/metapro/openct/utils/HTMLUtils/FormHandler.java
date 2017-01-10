package cc.metapro.openct.utils.HTMLUtils;

/*
 *  Copyright 2015 2017 metapro.cc Jeffctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FormHandler {

    // use linkedHashMap to ensure form seq
    private LinkedHashMap<String, List<Form>> mForms;

    public FormHandler(String html, String baseURL) {
        mForms = new LinkedHashMap<>();
        Document document = Jsoup.parse(html, baseURL);
        Elements elements = document.getElementsByTag("form");
        for (Element form : elements) {
            addForm(form);
        }
    }

    private void addForm(Element form) {
        String name = form.attr("name");
        List<Form> stored = mForms.get(name);
        if (stored == null) {
            List<Form> toAdd = new ArrayList<>();
            toAdd.add(new Form(form));
            mForms.put(name, toAdd);
        } else {
            stored.add(new Form(form));
        }
    }

    public Form getForm(int i) {
        int count = -1;
        for (List<Form> forms : mForms.values()) {
            for (Form s : forms) {
                count++;
                if (count == i) {
                    return s;
                }
            }
        }
        return null;
    }
}
