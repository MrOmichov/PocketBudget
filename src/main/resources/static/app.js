"use strict";
const $ = id => document.getElementById(id);
const state = {csrf:null, mode:"login", page:0, pages:0, filters:{}, deleting:null, version:0};
const money = value => {
  const n = BigInt(value), whole = (n / 100n).toLocaleString("ru-RU");
  return whole + "," + (n % 100n).toString().padStart(2,"0") + " ₽";
};
// Quote integer tokens before JSON.parse, leaving string literals untouched.
// This preserves bigint amounts and IDs even in browsers without JSON reviver context.
function parseExact(text) {
  return JSON.parse(text.replace(/"(?:\\.|[^"\\])*"|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
    token => token[0] === '"' || /[.eE]/.test(token) ? token : JSON.stringify(token)));
}
async function api(url, options={}) {
  const res = await fetch(url, {credentials:"same-origin", ...options});
  const text = await res.text();
  let data;
  try { data = text ? parseExact(text) : null; } catch { data = null; }
  if (!res.ok) {
    if (res.status === 401) showAuth();
    let message = ({400:"Проверьте введённые данные.",401:"Войдите в аккаунт.",403:"Срок действия формы истёк. Повторите действие.",404:"Запись не найдена.",409:"Это имя пользователя уже занято."})[res.status] || "Не удалось выполнить запрос.";
    if (res.status === 403) await loadCsrf().catch(()=>{});
    throw new Error(message);
  }
  return data;
}
async function loadCsrf() { state.csrf = await api("/api/auth/csrf"); }
function secured() { return {[state.csrf.headerName]:state.csrf.token}; }
function showAuth() {
  state.version++;
  $("dashboard").hidden=true; $("account").hidden=true; $("auth").hidden=false;
  $("expense-dialog").close(); $("delete-dialog").close();
  $("expenses").replaceChildren(); $("categories-summary").replaceChildren();
  $("auth-password").value="";
}
function mode(value) {
  state.mode=value;
  $("login-tab").setAttribute("aria-pressed",value==="login");
  $("register-tab").setAttribute("aria-pressed",value==="register");
  $("auth-submit").textContent=value==="login"?"Войти":"Создать аккаунт";
  $("auth-password").autocomplete=value==="login"?"current-password":"new-password";
  $("auth-password").minLength=value==="register"?8:1;
  $("auth-password").maxLength=value==="register"?64:1000;
  $("auth-error").textContent="";
}
async function enter() {
  const me=await api("/api/auth/me");
  const cats=await api("/api/categories");
  $("username").textContent=me.username;
  $("filter-category").replaceChildren(new Option("Все категории",""));
  $("add-category").replaceChildren();
  for (const cat of cats) {
    $("filter-category").add(new Option(cat.name,cat.id));
    $("add-category").add(new Option(cat.name,cat.id));
  }
  state.page=0; state.filters={}; $("filters").reset();
  $("auth").hidden=true; $("dashboard").hidden=false; $("account").hidden=false;
  $("auth-password").value=""; $("notice").textContent="";
  await refresh();
}
function query(paging) {
  const params=new URLSearchParams(state.filters);
  if(paging) { params.set("page",state.page);params.set("size","10"); }
  return params.toString();
}
async function refresh() {
  const version=++state.version;
  $("notice").textContent="";
  try {
    const [list,summary]=await Promise.all([
      api("/api/expenses?"+query(true)),api("/api/expenses/summary?"+query(false))
    ]);
    if(version!==state.version)return;
    if(state.page>0 && list.items.length===0 && Number(list.totalPages)<=state.page) {
      state.page=Math.max(0,Number(list.totalPages)-1);return refresh();
    }
    $("total").textContent=money(summary.totalAmount);
    $("count").textContent="Расходов: "+summary.expenseCount;
    $("list-count").textContent="Всего: "+list.totalElements;
    renderCategories(summary);
    $("expenses").replaceChildren();
    for(const item of list.items) {
      const tr=document.createElement("tr");
      const date=document.createElement("td");
      date.textContent=item.spentAt.split("-").reverse().join(".");
      const category=document.createElement("td");category.textContent=item.category;
      const amount=document.createElement("td");amount.className="number";amount.textContent=money(item.amount);
      const action=document.createElement("td"),button=document.createElement("button");
      button.className="delete-button";button.textContent="Удалить";
      button.setAttribute("aria-label","Удалить расход "+money(item.amount));
      button.onclick=()=>{state.deleting=item;$("delete-description").textContent=item.category+" · "+money(item.amount);$("delete-error").textContent="";$("delete-dialog").showModal();};
      action.append(button);tr.append(date,category,amount,action);$("expenses").append(tr);
    }
    $("empty").hidden=list.items.length!==0;
    state.pages=Number(list.totalPages);
    $("page-label").textContent=state.pages?"Страница "+(state.page+1)+" из "+state.pages:"Нет записей";
    $("prev").disabled=state.page===0;$("next").disabled=state.page+1>=state.pages;
  } catch(e) {if(version===state.version)$("notice").textContent=e.message;}
}
function renderCategories(summary) {
  const target=$("categories-summary");target.replaceChildren();
  if(!summary.categories.length){const p=document.createElement("p");p.className="muted";p.textContent="Здесь появятся итоги ваших расходов.";target.append(p);return;}
  const total=BigInt(summary.totalAmount);
  for(const item of summary.categories) {
    const row=document.createElement("div");row.className="category-row";
    const line=document.createElement("div");line.className="category-line";
    const name=document.createElement("span");name.textContent=item.category;
    const amount=document.createElement("span");amount.textContent=money(item.totalAmount);
    line.append(name,amount);
    const bar=document.createElement("div");bar.className="bar";
    const fill=document.createElement("div");fill.className="bar-fill";
    fill.style.width=(total?Number(BigInt(item.totalAmount)*10000n/total)/100:0)+"%";
    bar.append(fill);row.append(line,bar);target.append(row);
  }
}
$("login-tab").onclick=()=>mode("login");
$("register-tab").onclick=()=>mode("register");
$("auth-form").onsubmit=async event=>{
  event.preventDefault();$("auth-submit").disabled=true;$("auth-error").textContent="";
  try {
    await loadCsrf();
    const username=$("auth-user").value,password=$("auth-password").value;
    if(state.mode==="register"){
      await api("/api/auth/register",{method:"POST",headers:{"Content-Type":"application/json",...secured()},body:JSON.stringify({username,password})});
      mode("login");$("auth-password").value="";
      $("auth-error").textContent="Аккаунт создан. Теперь войдите.";
      return;
    }
    const response=await fetch("/login",{method:"POST",credentials:"same-origin",
      headers:{"Content-Type":"application/x-www-form-urlencoded",...secured()},
      body:new URLSearchParams({username,password})});
    if(!response.ok || new URL(response.url).pathname==="/login")throw new Error("Не удалось войти. Проверьте имя и пароль.");
    await loadCsrf();await enter();
  }catch(e){$("auth-error").textContent=e.message;}
  finally{$("auth-submit").disabled=false;}
};
$("logout").onclick=async()=>{
  $("logout").disabled=true;
  try {
    await loadCsrf();
    const response=await fetch("/logout",{method:"POST",credentials:"same-origin",headers:secured()});
    if(!response.ok)throw new Error("Не удалось выйти. Повторите попытку.");
    showAuth();await loadCsrf();
  }catch(e){$("notice").textContent=e.message;}
  finally{$("logout").disabled=false;}
};
$("filters").onsubmit=event=>{
  event.preventDefault();
  if($("from").value && $("to").value && $("from").value>$("to").value){$("notice").textContent="Начало периода должно быть не позже конца.";return;}
  state.filters={};
  for(const [key,id] of [["from","from"],["to","to"],["categoryId","filter-category"]])if($(id).value)state.filters[key]=$(id).value;
  state.page=0;refresh();
};
$("reset").onclick=()=>{$("filters").reset();state.filters={};state.page=0;refresh();};
$("prev").onclick=()=>{if(state.page>0){state.page--;refresh();}};
$("next").onclick=()=>{if(state.page+1<state.pages){state.page++;refresh();}};
$("add-open").onclick=()=>{
  $("expense-form").reset();$("expense-error").textContent="";
  const today=new Date();$("spent-at").value=[today.getFullYear(),String(today.getMonth()+1).padStart(2,"0"),String(today.getDate()).padStart(2,"0")].join("-");
  $("expense-dialog").showModal();$("amount").focus();
};
$("add-close").onclick=()=>$("expense-dialog").close();
$("expense-form").onsubmit=async event=>{
  event.preventDefault();$("expense-error").textContent="";
  const value=$("amount").value.trim().replace(",",".");
  if(!/^\d+(?:\.\d{1,2})?$/.test(value)){$("expense-error").textContent="Введите сумму с точностью до копейки, например 125,50.";return;}
  const [rub,kop=""]=value.split(".");
  const amount=BigInt(rub)*100n+BigInt(kop.padEnd(2,"0"));
  if(amount<1n||amount>9223372036854775807n){$("expense-error").textContent="Сумма должна быть от 0,01 до 92 233 720 368 547 758,07 ₽.";return;}
  $("save").disabled=true;
  try {
    await loadCsrf();
    await api("/api/expenses",{method:"POST",headers:{"Content-Type":"application/json",...secured()},
      body:'{"amount":'+amount.toString()+',"categoryId":'+JSON.stringify($("add-category").value)+',"spentAt":'+JSON.stringify($("spent-at").value)+'}'});
    $("expense-dialog").close();state.page=0;await refresh();
  }catch(e){$("expense-error").textContent=e.message;}
  finally{$("save").disabled=false;}
};
$("delete-cancel").onclick=()=>$("delete-dialog").close();
$("delete-form").onsubmit=async event=>{
  event.preventDefault();$("delete-confirm").disabled=true;
  try{
    await loadCsrf();await api("/api/expenses/"+encodeURIComponent(state.deleting.id),{method:"DELETE",headers:secured()});
    $("delete-dialog").close();await refresh();
  }catch(e){$("delete-error").textContent=e.message;}
  finally{$("delete-confirm").disabled=false;}
};
(async()=>{
  mode("login");
  try{
    await loadCsrf();
    const response=await fetch("/api/auth/me",{credentials:"same-origin"});
    if(response.status===401){showAuth();return;}
    if(!response.ok)throw new Error("Не удалось загрузить аккаунт.");
    await enter();
  }catch(e){showAuth();$("auth-error").textContent=e.message;}
})();
