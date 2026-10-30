// 한국어와 영어 중 하나만 보인다. 주소의 #en·#ko, 지난번에 고른 언어, 브라우저 언어 순으로 고른다.
// 스크립트가 돌지 않으면 두 언어가 다 보인다(legal.css).
(function () {
  var root = document.documentElement;
  var buttons = document.querySelectorAll('[data-set-lang]');

  function stored() {
    try { return localStorage.getItem('ovalit-legal-lang'); } catch (e) { return null; }
  }
  function store(lang) {
    try { localStorage.setItem('ovalit-legal-lang', lang); } catch (e) { /* 못 남겨도 된다 */ }
  }
  function apply(lang) {
    root.setAttribute('data-lang', lang);
    root.lang = lang;
    for (var i = 0; i < buttons.length; i++) {
      buttons[i].setAttribute('aria-pressed', String(buttons[i].getAttribute('data-set-lang') === lang));
    }
  }

  var hash = location.hash.replace('#', '');
  var browser = (navigator.language || '').toLowerCase().indexOf('ko') === 0 ? 'ko' : 'en';
  var lang = hash === 'en' || hash === 'ko' ? hash : (stored() || browser);
  // 삭제 요청 주소(#delete)로 들어왔는데 영어로 보이면 영어 쪽 같은 절로 옮긴다
  if (hash === 'delete' && lang === 'en') location.replace('#delete-en');
  if (hash === 'delete-en' && lang === 'ko') location.replace('#delete');
  apply(lang);

  for (var i = 0; i < buttons.length; i++) {
    buttons[i].addEventListener('click', function () {
      var next = this.getAttribute('data-set-lang');
      store(next);
      apply(next);
    });
  }
})();
