package cc.metapro.openct.university;


import android.support.annotation.NonNull;

import com.google.common.base.Strings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import cc.metapro.openct.data.BookInfo;
import cc.metapro.openct.data.BorrowInfo;
import cc.metapro.openct.utils.Constants;
import cc.metapro.openct.utils.HTMLUtils.Form;
import cc.metapro.openct.utils.HTMLUtils.FormHandler;
import cc.metapro.openct.utils.HTMLUtils.FormUtils;

public class LibraryFactory extends UniversityFactory {

    private static final Pattern nextPagePattern = Pattern.compile("(下一页)");

    private static String nextPageURL = "";

    private LibURLFactory mURLFactory;

    public LibraryFactory(
            @NonNull UniversityService service,
            @NonNull UniversityInfo.LibraryInfo libraryInfo
    ) {
        mBorrowTableInfo = libraryInfo.mBorrowTableInfo;
        mService = service;
        mLibraryInfo = libraryInfo;
        mURLFactory = new LibURLFactory(mLibraryInfo.mLibSys, libraryInfo.mLibURL);
    }

    @NonNull
    public List<BookInfo> search(@NonNull Map<String, String> searchMap) throws Exception {
        nextPageURL = "";
        String searchPage = mService.getPage(mURLFactory.SEARCH_REF, null).execute().body();

        FormHandler handler = new FormHandler(searchPage, mURLFactory.SEARCH_URL);
        Form form = handler.getForm(0);

        if (form == null) return new ArrayList<>(0);
        searchMap.put(Constants.SEARCH_TYPE, typeTrans(searchMap.get(Constants.SEARCH_CONTENT)));
        Map<String, String> res = FormUtils.getLibSearchQueryMap(form, searchMap);

        String action = res.get(Constants.ACTION);
        res.remove(Constants.ACTION);
        String resultPage = mService.searchLibrary(action, action, res).execute().body();

        prepareNextPageURL(resultPage);
        return Strings.isNullOrEmpty(resultPage) ? new ArrayList<BookInfo>(0) : parseBook(resultPage);
    }

    @NonNull
    public List<BookInfo> getNextPage() throws Exception {
        String resultPage = null;
        if (Strings.isNullOrEmpty(nextPageURL)) {
            return new ArrayList<>(0);
        }
        switch (mLibraryInfo.mLibSys) {
            case Constants.LIBSYS:
                resultPage = mService.getPage(nextPageURL, null).execute().body();
                break;
        }
        prepareNextPageURL(resultPage);
        return Strings.isNullOrEmpty(resultPage) ?
                new ArrayList<BookInfo>(0) : parseBook(resultPage);
    }

    private void prepareNextPageURL(String resultPage) {
        Document result = Jsoup.parse(resultPage, mURLFactory.SEARCH_URL);
        Elements links = result.select("a");
        for (Element a : links) {
            if (nextPagePattern.matcher(a.text()).find()) {
                String tmp = a.absUrl("href");
                if (!tmp.equals(nextPageURL)) {
                    nextPageURL = tmp;
                } else {
                    nextPageURL = "";
                }
                break;
            }
        }
    }

    @NonNull
    public List<BorrowInfo> getBorrowInfo  (
            @NonNull Map<String, String> loginMap
    ) throws Exception {
        String page = login(loginMap);
        String borrowPage = null;
        switch (mLibraryInfo.mLibSys) {
            case Constants.LIBSYS:
                borrowPage = mService
                        .getPage(mURLFactory.BORROW_URL, mURLFactory.USER_HOME_URL)
                        .execute().body();
                break;
        }
        return Strings.isNullOrEmpty(borrowPage) ? new ArrayList<BorrowInfo>(0) : parseBorrow(borrowPage);
    }

    @NonNull
    private String typeTrans(String cnType) {
        switch (cnType) {
            case "书名":
                return "title";
            case "作者":
                return "author";
            case "ISBN":
                return "isbn";
            case "出版社":
                return "publisher";
            case "丛书名":
                return "series";
            default:
                return "title";
        }
    }

    @NonNull
    private List<BookInfo> parseBook(@NonNull String resultPage) {
        List<BookInfo> bookInfos = new ArrayList<>();
        Document document = Jsoup.parse(resultPage, mURLFactory.SEARCH_URL);
        Elements elements = document.select("li[class=book_list_info]");
        Elements tmp = document.select("div[class=list_books]");
        elements.addAll(tmp);
        for (Element entry : elements) {
            Elements els_title = entry.children().select("h3");
            String tmp_1 = els_title.text();
            String title = els_title.select("a").text();
            String href = els_title.select("a").get(0).absUrl("href");

            if (Strings.isNullOrEmpty(title)) return new ArrayList<>(0);

            title = title.split("\\.")[1];
            String[] tmps = tmp_1.split(" ");
            String content = tmps[tmps.length - 1];
            Elements els_body = entry.children().select("p");
            String author = els_body.text();
            els_body = els_body.select("span");
            String remains = els_body.text();
            author = author.substring(author.indexOf(remains) + remains.length());
            BookInfo b = new BookInfo(title, author, content, remains, href);
            bookInfos.add(b);
        }
        return bookInfos;
    }

    @NonNull
    private List<BorrowInfo> parseBorrow(@NonNull String resultPage) {
        List<BorrowInfo> list = new ArrayList<>();
        Document doc = Jsoup.parse(resultPage);
        Elements elements = doc.select("table");
        for (Element e : elements) {
            if (e.attr("class").equals(mBorrowTableInfo.mTableID)) {
                Elements trs = e.select("tr");
                trs.remove(0);
                for (Element tr : trs) {
                    Elements entry = tr.select("td");
                    String title = entry.get(mBorrowTableInfo.mTitleIndex).text().split("/")[0];
                    String author = entry.get(mBorrowTableInfo.mTitleIndex).text().split("/")[1];
                    BorrowInfo info = new BorrowInfo(
                            entry.get(mBorrowTableInfo.mBarcodeIndex).text(),
                            title, author, "",
                            entry.get(mBorrowTableInfo.mBorrowDateIndex).text(),
                            entry.get(mBorrowTableInfo.mDueDateIndexIndex).text());
                    list.add(info);
                }
                return list;
            }
        }
        return new ArrayList<>(0);
    }

    @Override
    protected String getCaptchaURL() {
        return mURLFactory.CAPTCHA_URL;
    }

    @Override
    protected String getLoginURL() {
        return mURLFactory.LOGIN_URL;
    }

    @Override
    protected String getLoginReferer() {
        return mURLFactory.LOGIN_REF;
    }

    @Override
    protected void resetURLFactory() {
        mURLFactory = new LibURLFactory(mLibraryInfo.mLibSys, mLibraryInfo.mLibURL + "/" + dynPart + "/");
    }

    public static class BorrowTableInfo {
        public int
                mBarcodeIndex,
                mTitleIndex,
                mBorrowDateIndex,
                mDueDateIndexIndex;
        public String mTableID;
    }

    private class LibURLFactory {

        String
                LOGIN_URL, LOGIN_REF,
                SEARCH_URL, SEARCH_REF,
                CAPTCHA_URL, USER_HOME_URL,
                BORROW_URL;

        LibURLFactory(@NonNull String libSys, @NonNull String libBaseURL) {
            if (!libBaseURL.endsWith("/")) {
                libBaseURL += "/";
            }

            switch (libSys) {
                case Constants.LIBSYS:
                    SEARCH_URL = libBaseURL + "opac/openlink.php?";
                    SEARCH_REF = libBaseURL + "opac/search.php";
                    CAPTCHA_URL = libBaseURL + "reader/captcha.php";
                    LOGIN_URL = libBaseURL + "reader/redr_verify.php";
                    LOGIN_REF = libBaseURL + "reader/login.php";
                    USER_HOME_URL = libBaseURL + "reader/redr_info.php";
                    BORROW_URL = libBaseURL + "reader/book_lst.php";
                    break;
            }
        }
    }
}
