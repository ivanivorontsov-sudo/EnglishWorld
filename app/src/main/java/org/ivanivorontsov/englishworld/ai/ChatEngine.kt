package org.ivanivorontsov.englishworld.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class ChatTurn(val role: String, val content: String)
data class ApiSettings(val baseUrl: String, val model: String, val apiKey: String)
data class ChatProvider(val id: String, val title: String, val url: String? = null) {
    val isWebChat: Boolean get() = url != null
}

object ChatEngine {
    val providers = listOf(
        ChatProvider("qwen", "Qwen · аккаунт", "https://chat.qwen.ai/"),
        ChatProvider("chatgpt", "ChatGPT · аккаунт", "https://chatgpt.com/"),
        ChatProvider("gemini", "Gemini · аккаунт", "https://gemini.google.com/app"),
        ChatProvider("grok", "Grok · аккаунт", "https://grok.com/"),
        ChatProvider("copilot", "Copilot · аккаунт", "https://copilot.microsoft.com/"),
        ChatProvider("api", "Свой ИИ-сервер · API")
    )

    fun tutorInstruction(level: String): String = """
You are EnglishWorld, a friendly and highly effective English speaking tutor.
The learner's current CEFR level is $level and the target is B2. The learner's support language is Russian.
Reply to the actual question first. The learner may ask ANY question about English: grammar, vocabulary, idioms, pronunciation, writing, listening, exams or usage.
Keep the natural spoken answer concise (normally 2–5 sentences), then coach one useful improvement.
Be warm, curious and respectful. Never shame the learner or over-correct every small slip.
When the learner makes an error, show the original briefly, give a natural corrected version, and explain the most important reason in Russian.
Offer natural collocations and chunks, not isolated word lists.
Ask exactly one relevant follow-up question to keep the learner speaking.
Adapt sentence complexity to $level, gradually stretching the learner toward B2.
For pronunciation, give a simple IPA hint only when useful. Never claim to hear sounds that speech-to-text cannot capture.
For uncertain or regional usage, say so and distinguish common British and American forms.
Never invent a grammar rule, exam score or pronunciation diagnosis.
Return this compact structure:
[SPEAK]
A natural answer in English that should be read aloud.
[/SPEAK]
[COACH]
One correction or teaching point in Russian. If no correction is needed, give one useful nuance.
[/COACH]
[PHRASES]
Up to three useful English chunks, each with a brief meaning.
[/PHRASES]
[NEXT]
Exactly one short follow-up question in English.
[/NEXT]
The SPEAK and NEXT sections must contain English only. Avoid markdown tables.
""".trimIndent()

    fun learnerPrompt(question: String, level: String): String =
        tutorInstruction(level) + "\n\nThe learner says or asks:\n" + question

    fun spokenText(answer: String): String {
        val flags = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        val speak = Regex("""\[SPEAK\](.*?)\[/SPEAK\]""", flags).find(answer)?.groupValues?.get(1)?.trim()
        val next = Regex("""\[NEXT\](.*?)\[/NEXT\]""", flags).find(answer)?.groupValues?.get(1)?.trim()
        return listOfNotNull(speak, next).filter { it.isNotBlank() }.joinToString(". ")
            .ifBlank { answer.take(900) }
    }

    suspend fun askApi(settings: ApiSettings, level: String, previous: List<ChatTurn>, userText: String): String =
        withContext(Dispatchers.IO) {
            require(settings.apiKey.isNotBlank()) { "Добавьте API-ключ или выберите веб-чат." }
            require(settings.model.isNotBlank()) { "Укажите модель в настройках." }
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", tutorInstruction(level)))
            previous.takeLast(10).forEach { turn ->
                if (turn.role == "user" || turn.role == "assistant") {
                    messages.put(JSONObject().put("role", turn.role).put("content", turn.content))
                }
            }
            messages.put(JSONObject().put("role", "user").put("content", userText))
            val payload = JSONObject()
                .put("model", settings.model)
                .put("messages", messages)
                .put("temperature", 0.45)
                .put("max_tokens", 750)
            val connection = (URL(chatCompletionsUrl(settings.baseUrl)).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val detail = runCatching { JSONObject(body).optString("error", body) }.getOrDefault(body)
                    throw IllegalStateException("ИИ-сервер: HTTP $code — ${detail.take(250)}")
                }
                val content = JSONObject(body).optJSONArray("choices")
                    ?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
                val answer = when (content) {
                    is String -> content
                    is JSONArray -> buildString {
                        for (i in 0 until content.length()) append(content.optJSONObject(i)?.optString("text").orEmpty())
                    }
                    else -> ""
                }.trim()
                if (answer.isBlank()) throw IllegalStateException("Модель вернула пустой ответ.")
                answer
            } finally {
                connection.disconnect()
            }
        }

    fun chatCompletionsUrl(base: String): String {
        val clean = base.trim().trimEnd('/')
        require(clean.startsWith("https://")) { "Адрес сервера должен начинаться с https://" }
        return when {
            clean.endsWith("/chat/completions") -> clean
            clean.endsWith("/v1") -> "$clean/chat/completions"
            else -> "$clean/v1/chat/completions"
        }
    }

    private const val HELPERS = """
        function ewVisible(e){
          if(!e||!e.getBoundingClientRect)return false;
          var r=e.getBoundingClientRect(),s=window.getComputedStyle?getComputedStyle(e):null;
          return r.width>0&&(r.height>0||e.isContentEditable)&&(!s||(s.visibility!=='hidden'&&s.display!=='none'));
        }
        function ewInput(){
          var sels=['#prompt-textarea','div.ProseMirror[contenteditable=true]','textarea#chat-input','#chat-input',
            'rich-textarea .ql-editor','.ql-editor[contenteditable=true]','textarea[placeholder]','textarea',
            '[contenteditable=true][role=textbox]','[contenteditable=true]'];
          for(var i=0;i<sels.length;i++){var a=document.querySelectorAll(sels[i]);for(var j=0;j<a.length;j++)if(ewVisible(a[j]))return a[j];}
          return null;
        }
        function ewText(e){return e?((e.value!==undefined&&e.tagName!=='DIV')?e.value:(e.innerText||e.textContent||'')):'';}
    """
    val STATE_SCRIPT: String = """(function(){ $HELPERS
      var p=document.querySelector('input[type=password]');return ((p&&ewVisible(p))||!ewInput())?'login':'ok';
    })()"""

    fun injectScript(prompt: String): String {
        val quoted = jsonQuote(prompt)
        return """(function(){ $HELPERS
          var p=$quoted,el=ewInput();if(!el)return 'noinput';el.focus();
          if(el.tagName==='TEXTAREA'||el.tagName==='INPUT'){
            var proto=el.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;
            var setter=Object.getOwnPropertyDescriptor(proto,'value').set;setter.call(el,p);
            try{el.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:p}));}
            catch(e){el.dispatchEvent(new Event('input',{bubbles:true}));}
            el.dispatchEvent(new Event('change',{bubbles:true}));
          }else{
            try{var sel=window.getSelection(),r=document.createRange();r.selectNodeContents(el);sel.removeAllRanges();sel.addRange(r);}catch(e){}
            var ok=false;try{ok=document.execCommand('insertText',false,p);}catch(e){}
            if(!ok||ewText(el).indexOf(p.slice(0,16))<0){
              try{el.dispatchEvent(new InputEvent('beforeinput',{bubbles:true,cancelable:true,inputType:'insertText',data:p}));}catch(e){}
              if(ewText(el).indexOf(p.slice(0,16))<0){while(el.firstChild)el.removeChild(el.firstChild);var para=document.createElement('p');para.textContent=p;el.appendChild(para);}
              try{el.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:p}));}catch(e){}
            }
          }
          window.__ewPrompt=p;window.__ewLength=0;window.__ewStable=0;
          return ewText(el).indexOf(p.slice(0,16))>=0?'ok':'typed?';
        })()"""
    }

    val SUBMIT_SCRIPT: String = """(function(){ $HELPERS
      var el=ewInput();if(!el)return 'noinput';
      window.__ewOld=[];
      var oldNodes=document.querySelectorAll('[data-message-author-role=assistant],[data-testid*=assistant],.markdown,article');
      for(var n=0;n<oldNodes.length;n++){var oldText=ewText(oldNodes[n]).trim();if(oldText.length>10)window.__ewOld.push(oldText);}
      function usable(b){return ewVisible(b)&&!b.disabled&&b.getAttribute('aria-disabled')!=='true';}
      var sels=['button[data-testid=send-button]','#composer-submit-button','#send-message-button',
        'button[aria-label*=Send i]','button[aria-label*=Отправ i]','button[type=submit]'];
      for(var i=0;i<sels.length;i++){var a=document.querySelectorAll(sels[i]);for(var j=0;j<a.length;j++)if(usable(a[j])){a[j].click();return 'clicked';}}
      el.focus();var o={key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true};
      el.dispatchEvent(new KeyboardEvent('keydown',o));el.dispatchEvent(new KeyboardEvent('keyup',o));return 'enter';
    })()"""

    val READ_REPLY_SCRIPT: String = """(function(){ $HELPERS
      var p=window.__ewPrompt||'',sels=['[data-message-author-role=assistant]','[data-testid*=assistant]',
        '.markdown','article','[class*=assistant]','[class*=answer]','[class*=response]'];
      var best='';
      function consider(t){t=(t||'').trim();if(t.length<8)return;if(p&&t.indexOf(p.slice(0,36))===0)return;if(window.__ewOld&&window.__ewOld.indexOf(t)>=0)return;if(t.length>best.length)best=t;}
      for(var i=0;i<sels.length;i++){var a=document.querySelectorAll(sels[i]);for(var j=0;j<a.length;j++)if(ewVisible(a[j]))consider(ewText(a[j]));}
      var len=best.length;
      if(len>0&&len===window.__ewLength)window.__ewStable=(window.__ewStable||0)+1;else window.__ewStable=0;
      window.__ewLength=len;
      return JSON.stringify({state:(!ewInput()&&len===0)?'login':'ok',text:best,stable:window.__ewStable>=3});
    })()"""

    fun jsonQuote(value: String): String = JSONObject.quote(value)
    fun parseJavascriptString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        return runCatching { JSONTokener(raw).nextValue() as? String ?: raw }.getOrDefault(raw)
    }
}
