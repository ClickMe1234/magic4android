const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {EventEmitter} = require('node:events');
const path = require('node:path');

function fixture() {
  const subscriptions = [], timers = [], sent = [];
  class Service {
    call() {}
    register() {}
    subscribe() {
      const handle = new EventEmitter();
      handle.cancel = () => handle.emit('cancel');
      subscriptions.push(handle);
      return handle;
    }
  }
  let now = 10000;
  const context = vm.createContext({
    require: name => name === './package.json' ? {name:'test'} : name === 'webos-service' ? Service : require(name),
    Buffer, console, Date: {now: () => now},
    setInterval: () => 1, clearInterval() {},
    setTimeout: (fn, delay) => timers.push({fn, delay}),
    client: Object.assign(new EventEmitter(), {send: text => sent.push(JSON.parse(text)), close() {}})
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../service/m4p_service.js'), 'utf8'), context);
  vm.runInContext('startUnicastingData(client, {address:"192.168.1.2",port:1234}, {filter:["coordinate"]})', context);
  return {context, subscriptions, timers, sent, tick: () => {now += 100;}};
}

test('subscription acknowledgement is not mistaken for a coordinate sample', () => {
  const f = fixture();
  f.subscriptions[0].emit('response', {payload:{returnValue:true,subscribed:true}});
  assert.equal(f.context.log, '');
  assert.equal(f.sent.filter(p => p.t === 'remote_update').length, 0);
  assert.equal(f.timers.length, 0);
});

test('remote-not-ready retries once after a delay, then forwards recovered coordinates', () => {
  const f = fixture();
  f.subscriptions[0].emit('response', {payload:{returnValue:false,errorText:'Magic Remote is not Ready'}});
  assert.equal(f.context.sensorStatus, 'Magic Remote is not Ready');
  assert.equal(f.timers.length, 1);
  assert.equal(f.timers[0].delay, 1000);
  f.timers[0].fn();
  assert.equal(f.subscriptions.length, 2);
  f.tick();
  f.subscriptions[1].emit('response', {payload:{returnValue:true,coordinate:{x:123,y:456}}});
  const packet = f.sent.find(p => p.t === 'remote_update');
  const bytes = Buffer.from(packet.payload, 'base64');
  assert.equal(bytes.readInt32LE(0), 123);
  assert.equal(bytes.readInt32LE(4), 456);
  assert.equal(f.context.sensorStatus, 'Receiving');
});

test('pending sensor retry does not restart a disconnected session', () => {
  const f = fixture();
  f.subscriptions[0].emit('cancel');
  f.context.unicastDataActive = false;
  f.timers[0].fn();
  assert.equal(f.subscriptions.length, 1);
});
